import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

# Model
BASE_MODEL_NAME: str = "meta-llama/Llama-3.1-8B-Instruct"
ADAPTER_PATH: Path = Path(__file__).parent.parent / "Treinamento" / "llama-backlog-lora"

SYSTEM_PROMPT: str = "Convert project description into structured GitHub issues JSON."

# Generation
MAX_NEW_TOKENS: int = 5000
TEMPERATURE: float = 0.4
TOP_P: float = 0.9
REPETITION_PENALTY: float = 1.2

# Queue
# Increase MAX_CONCURRENT cautiously: concurrent GPU inferences share VRAM and
# compute, so each request will take proportionally longer as this value grows.
MAX_CONCURRENT: int = 2
REQUEST_TIMEOUT_SECONDS: float = 1800.0
MAX_GENERATION_RETRIES: int = 3

# Callback URL for sending results back to the client
CALLBACK_URL: str = "http://localhost:8080/v1/ia/callback"
CALLBACK_JWT: str = os.getenv("JWT", "")
