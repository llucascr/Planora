import asyncio

from fastapi import APIRouter, HTTPException, Request, status

from queue_manager import InferenceQueueManager
from schemas import AcceptedResponse, BacklogRequest, BacklogResponse, HealthResponse, QueueStatusResponse

router = APIRouter(prefix="/api/v1", tags=["backlog"])


@router.post(
    "/generate-backlog",
    response_model=AcceptedResponse,
    summary="Generate a structured GitHub issues backlog from a project description.",
    status_code=status.HTTP_202_ACCEPTED
)
async def generate_backlog(request: Request, body: BacklogRequest) -> AcceptedResponse:
    queue_manager: InferenceQueueManager = request.app.state.queue_manager

    try:
        await queue_manager.submit_with_callback(body.description, body.jobId)
        return AcceptedResponse(
            jobId=body.jobId,
            message="Backlog generation submitted successfully."
        )

    except asyncio.TimeoutError:
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="Request timed out while waiting for inference to complete.",
        )

    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference failed: {exc}",
        )


@router.get(
    "/status",
    response_model=QueueStatusResponse,
    summary="Return current queue occupancy and availability.",
)
async def get_status(request: Request) -> QueueStatusResponse:
    queue_manager: InferenceQueueManager = request.app.state.queue_manager
    return QueueStatusResponse(
        pending_requests=queue_manager.queue_size,
        active_workers=queue_manager.active_workers,
    )


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Return service health and model load state.",
)
async def health_check(request: Request) -> HealthResponse:
    model_service = request.app.state.model_service
    return HealthResponse(
        status="ok",
        model_loaded=model_service.is_loaded,
    )
