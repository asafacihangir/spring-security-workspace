package org.phoenix.springsecurity.web.api;

import java.util.List;
import java.util.Map;

import org.phoenix.springsecurity.service.WorkLogService;
import org.phoenix.springsecurity.web.api.dto.CreateWorkLogRequest;
import org.phoenix.springsecurity.web.api.dto.WorkLogDto;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-logs")
public class WorkLogsRestController {

	private final WorkLogService workLogService;

	public WorkLogsRestController(WorkLogService workLogService) {
		this.workLogService = workLogService;
	}

	@GetMapping
	public List<WorkLogDto> allWorkLogs() {
		return workLogService.getAll().stream().map(WorkLogDto::from).toList();
	}

	@GetMapping("/my")
	public List<WorkLogDto> myWorkLogs() {
		return workLogService.findMine().stream().map(WorkLogDto::from).toList();
	}

	@GetMapping("/{workLogId}")
	public WorkLogDto show(@PathVariable int workLogId) {
		return WorkLogDto.from(workLogService.get(workLogId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Integer> create(@Valid @RequestBody CreateWorkLogRequest request) {
		return Map.of("id", workLogService.create(request.explanation()));
	}

}
