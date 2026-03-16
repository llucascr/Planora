package com.planora.backend.service;

import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LabelService {

    private final LabelRepository labelRepository;

    public Label resolveOrCreateLabel(LabelResponse labelResponse) {
        return labelRepository.findByName(labelResponse.name())
                .orElseGet(() -> {
                    Label label = new Label();
                    label.setUrl(labelResponse.url());
                    label.setName(labelResponse.name());
                    label.setColor(labelResponse.color());
                    label.setDescription(labelResponse.description());
                    return labelRepository.save(label);
                });
    }

}
