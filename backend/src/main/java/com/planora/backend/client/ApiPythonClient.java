package com.planora.backend.client;

import com.planora.backend.model.issue.dto.AcceptedResponse;
import com.planora.backend.model.issue.dto.BacklogRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ApiPythonClient {

    @PostExchange("/generate-backlog")
    AcceptedResponse generateBacklog(
            @RequestBody BacklogRequest body
    );

}
