from pydantic import BaseModel, Field


class BacklogRequest(BaseModel):
    description: str = Field(
        ...,
        min_length=10,
        description="Project description to be converted into structured GitHub issues."
    )
    jobId: int = Field(
        ...,
        description="The ID of the job for which to generate the backlog."
    )


class BacklogResponse(BaseModel):
    backlog: list[dict]
    jobId: int

class AcceptedResponse(BaseModel):
    jobId: int
    message: str

class QueueStatusResponse(BaseModel):
    pending_requests: int
    active_workers: int


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
