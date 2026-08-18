package org.phoenix.springsecurity.web.api.dto;

import java.time.format.DateTimeFormatter;

import org.phoenix.springsecurity.domain.WorkLog;

public record WorkLogDto(Integer id, String explanation, String createdDate, Integer createdBy) {

	private static final DateTimeFormatter CREATED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	public static WorkLogDto from(WorkLog workLog) {
		String createdDate = workLog.getCreatedDate() == null ? null
				: CREATED_DATE_FORMAT.format(workLog.getCreatedDate());
		return new WorkLogDto(workLog.getId(), workLog.getExplanation(), createdDate, workLog.getCreatedBy());
	}
}
