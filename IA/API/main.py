from contextlib import asynccontextmanager

from fastapi import FastAPI

from model_service import ModelService
from queue_manager import InferenceQueueManager
from router import router


@asynccontextmanager
async def lifespan(app: FastAPI):
    model_service = ModelService()
    model_service.load()

    queue_manager = InferenceQueueManager(model_service)
    await queue_manager.start()

    app.state.model_service = model_service
    app.state.queue_manager = queue_manager

    yield

    await queue_manager.stop()
    model_service.unload()


app = FastAPI(
    title="Planora Backlog Generator",
    description=(
        "Generates structured GitHub issues backlogs from project descriptions "
        "using a fine-tuned LLaMA 3.1 8B model."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000)
