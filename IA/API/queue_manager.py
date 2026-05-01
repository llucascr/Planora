import asyncio
from concurrent.futures import ThreadPoolExecutor
from typing import Tuple

from model_service import ModelService
import config


class InferenceQueueManager:
    """
    Manages an unbounded async queue of inference requests processed by a
    configurable worker pool.

    Each worker runs GPU inference in a thread executor so the event loop is
    never blocked. The number of concurrent inferences is controlled by
    config.MAX_CONCURRENT — increasing it allows parallelism at the cost of
    each individual request taking longer due to shared GPU resources.
    """

    def __init__(self, model_service: ModelService) -> None:
        self._model_service = model_service
        self._queue: asyncio.Queue[Tuple[str, asyncio.Future]] = asyncio.Queue()
        self._executor = ThreadPoolExecutor(max_workers=config.MAX_CONCURRENT)
        self._worker_tasks: list[asyncio.Task] = []

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    async def start(self) -> None:
        """Spawn one worker task per allowed concurrent slot."""
        self._worker_tasks = [
            asyncio.create_task(self._worker(), name=f"inference-worker-{i}")
            for i in range(config.MAX_CONCURRENT)
        ]

    async def stop(self) -> None:
        """Cancel all workers and reject requests still waiting in the queue."""
        for task in self._worker_tasks:
            task.cancel()
        await asyncio.gather(*self._worker_tasks, return_exceptions=True)

        while not self._queue.empty():
            try:
                _, future = self._queue.get_nowait()
                if not future.done():
                    future.cancel()
            except asyncio.QueueEmpty:
                break

        self._executor.shutdown(wait=False)

    # ------------------------------------------------------------------
    # Public interface
    # ------------------------------------------------------------------

    async def submit(self, description: str) -> str:
        """
        Enqueue an inference request and await its result.

        Raises:
            asyncio.TimeoutError: when the request exceeds REQUEST_TIMEOUT_SECONDS.
        """
        loop = asyncio.get_running_loop()
        future: asyncio.Future[str] = loop.create_future()

        await self._queue.put((description, future))

        return await asyncio.wait_for(future, timeout=config.REQUEST_TIMEOUT_SECONDS)

    @property
    def queue_size(self) -> int:
        return self._queue.qsize()

    @property
    def active_workers(self) -> int:
        return config.MAX_CONCURRENT

    # ------------------------------------------------------------------
    # Internal worker
    # ------------------------------------------------------------------

    async def _worker(self) -> None:
        """
        Drains the queue independently. Multiple instances of this coroutine
        run concurrently, each pulling one item at a time.
        """
        loop = asyncio.get_running_loop()

        while True:
            description, future = await self._queue.get()
            try:
                if future.done():
                    continue

                result = await loop.run_in_executor(
                    self._executor,
                    self._model_service.generate,
                    description,
                )

                if not future.done():
                    future.set_result(result)

            except asyncio.CancelledError:
                if not future.done():
                    future.cancel()
                raise

            except Exception as exc:
                if not future.done():
                    future.set_exception(exc)

            finally:
                self._queue.task_done()
