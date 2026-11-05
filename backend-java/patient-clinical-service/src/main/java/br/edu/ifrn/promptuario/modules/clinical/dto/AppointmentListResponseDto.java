package br.edu.ifrn.promptuario.modules.clinical.dto;
import java.util.List;
public record AppointmentListResponseDto(List<AppointmentResponseDto> items, long total, int page, int size) {
}
