import torch
import json
import re
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import PeftModel

import config


class ModelService:
    def __init__(self) -> None:
        self._model: PeftModel | None = None
        self._tokenizer: AutoTokenizer | None = None
        self._eot_token_id: int | None = None
        self._is_loaded: bool = False

    def _parse_output(self, rawText: str) -> list[dict]:
        # strip markdown code fences se o modelo gerar ```json ... ```
        text = re.sub(r"^```(?:json)?\s*", "", rawText.strip())
        text = re.sub(r"\s*```$", "", text)
        return json.loads(text)

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded

    def load(self) -> None:
        """Load the quantized base model and LoRA adapter into GPU memory."""
        bnb_config = BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_compute_dtype=torch.float16,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_use_double_quant=True,
        )

        self._tokenizer = AutoTokenizer.from_pretrained(config.BASE_MODEL_NAME)
        self._tokenizer.pad_token = self._tokenizer.eos_token

        base_model = AutoModelForCausalLM.from_pretrained(
            config.BASE_MODEL_NAME,
            quantization_config=bnb_config,
            device_map="cuda",
            torch_dtype=torch.float16,
        )

        self._model = PeftModel.from_pretrained(base_model, str(config.ADAPTER_PATH))
        self._model.eval()

        self._eot_token_id = self._tokenizer.convert_tokens_to_ids("<|eot_id|>")
        self._is_loaded = True

    def generate(self, description: str) -> list[dict]:
        """Run synchronous inference. Intended to be called from a thread executor."""
        if not self._is_loaded:
            raise RuntimeError("Model has not been loaded. Call load() first.")

        messages = [
            {"role": "system", "content": config.SYSTEM_PROMPT},
            {"role": "user", "content": description},
        ]

        prompt = self._tokenizer.apply_chat_template(
            messages, tokenize=False, add_generation_prompt=True
        )
        inputs = self._tokenizer(prompt, return_tensors="pt").to("cuda")

        with torch.no_grad():
            output = self._model.generate(
                **inputs,
                do_sample=True,
                temperature=config.TEMPERATURE,
                top_p=config.TOP_P,
                repetition_penalty=config.REPETITION_PENALTY,
                max_new_tokens=config.MAX_NEW_TOKENS,
                eos_token_id=[self._tokenizer.eos_token_id, self._eot_token_id],
                pad_token_id=self._tokenizer.eos_token_id,
            )

        raw = self._tokenizer.decode(
            output[0][inputs["input_ids"].shape[-1]:],
            skip_special_tokens=True,
        )

        return self._parse_output(raw)

    def unload(self) -> None:
        """Release model weights and free GPU memory."""
        del self._model
        del self._tokenizer
        self._model = None
        self._tokenizer = None
        self._eot_token_id = None
        self._is_loaded = False

        if torch.cuda.is_available():
            torch.cuda.empty_cache()
