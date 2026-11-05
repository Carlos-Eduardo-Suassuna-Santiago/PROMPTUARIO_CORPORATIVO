package br.edu.ifrn.promptuario.modules.clinical.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
public record ExamResultUpdateDto(@NotBlank @Size(min = 5) String result, Instant resultDate) {
}
