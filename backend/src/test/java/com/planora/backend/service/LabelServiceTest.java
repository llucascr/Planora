package com.planora.backend.service;

import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.repository.LabelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LabelService")
class LabelServiceTest {

    private static final String LABEL_NAME = "bug";
    private static final String LABEL_URL = "https://api.github.com/repos/owner/repo/labels/bug";
    private static final String LABEL_COLOR = "f00";
    private static final String LABEL_DESCRIPTION = "Algo de errado";

    @Mock private LabelRepository labelRepository;

    @InjectMocks private LabelService labelService;

    private LabelResponse buildLabelResponse() {
        return new LabelResponse(LABEL_URL, LABEL_NAME, LABEL_COLOR, LABEL_DESCRIPTION);
    }

    @Nested
    @DisplayName("resolveOrCreateLabel")
    class ResolveOrCreateLabel {

        @Test
        @DisplayName("deve retornar label existente quando label já existe pelo nome")
        void deveRetornarLabelExistente_quandoLabelJaExistePeloNome() {
            Label existing = new Label();
            existing.setLabelId(10L);
            existing.setName(LABEL_NAME);
            when(labelRepository.findByName(LABEL_NAME)).thenReturn(Optional.of(existing));

            Label result = labelService.resolveOrCreateLabel(buildLabelResponse());

            assertThat(result).isSameAs(existing);
            verify(labelRepository, never()).save(any(Label.class));
        }

        @Test
        @DisplayName("deve criar e persistir nova label quando não existe")
        void deveCriarEPersistirNovaLabel_quandoNaoExiste() {
            when(labelRepository.findByName(LABEL_NAME)).thenReturn(Optional.empty());
            when(labelRepository.save(any(Label.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Label result = labelService.resolveOrCreateLabel(buildLabelResponse());

            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            Label saved = labelCaptor.getValue();

            assertThat(saved.getName()).isEqualTo(LABEL_NAME);
            assertThat(saved.getUrl()).isEqualTo(LABEL_URL);
            assertThat(saved.getColor()).isEqualTo(LABEL_COLOR);
            assertThat(saved.getDescription()).isEqualTo(LABEL_DESCRIPTION);
            assertThat(result).isSameAs(saved);
        }
    }
}
