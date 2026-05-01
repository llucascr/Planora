from pathlib import Path

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
