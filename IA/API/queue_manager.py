import asyncio
from concurrent.futures import ThreadPoolExecutor
from typing import Tuple

import httpx
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
        self._queue: asyncio.Queue[Tuple[str, asyncio.Future[list[dict]]]] = asyncio.Queue()
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

    async def submit(self, description: str) -> list[dict]:
        """
        Enqueue an inference request and await its result.

        Raises:
            asyncio.TimeoutError: when the request exceeds REQUEST_TIMEOUT_SECONDS.
        """
        loop = asyncio.get_running_loop()
        future: asyncio.Future[list[dict]] = loop.create_future()

        await self._queue.put((description, future))

        return await asyncio.wait_for(future, timeout=config.REQUEST_TIMEOUT_SECONDS)
    
    # New logic with callback support
    async def submit_with_callback(self, description: str, job_id: int) -> None:
        loop = asyncio.get_running_loop()
        future: asyncio.Future[list[dict]] = loop.create_future()
        await self._queue.put((description, future))
        # cria uma task que aguarda o resultado e dispara o callback
        asyncio.create_task(self._await_and_callback(future, job_id))

    async def _await_and_callback(self, future: asyncio.Future, job_id: int) -> None:
        try:
            result = await future
            payload = {"backlog": result, "jobId": job_id}
        except Exception as exc:
            payload = {"error": str(exc), "jobId": job_id}
        await self._post_callback(payload)

    async def _post_callback(self, payload: dict) -> None:
        async with httpx.AsyncClient() as client:
            await client.post(config.CALLBACK_URL, json=payload, timeout=30.0)

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
