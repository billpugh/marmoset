package edu.umd.review.gwt.rpc.dto;

import edu.umd.cs.marmoset.modelClasses.CodeReviewThread;
import edu.umd.cs.marmoset.modelClasses.CodeReviewer;
import edu.umd.cs.marmoset.modelClasses.RubricEvaluation;
import edu.umd.cs.marmoset.review.UniqueReviewerName;

public class CheckboxEvaluationDto
		extends RubricEvaluationDto {

	@Deprecated @SuppressWarnings("unused")
	private CheckboxEvaluationDto() {
		super();
  }

	public CheckboxEvaluationDto(CheckboxRubricDto rubric,
	                             @RubricEvaluation.PK int rubricEvaluationPK,
	                             @CodeReviewer.PK int authorPK,
	                             @UniqueReviewerName String authorName,
	                             @CodeReviewThread.PK int threadid,
	                             boolean editable, Status status) {
		super(rubric, rubricEvaluationPK, authorPK, authorName, threadid, editable, status);
	}
}
