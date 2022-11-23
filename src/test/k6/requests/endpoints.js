export const PROGRAMMING_EXERCISES_SETUP = '/programming-exercises/setup';
export const PROGRAMMING_EXERCISES = '/programming-exercises';
export const PROGRAMMING_EXERCISE = (exerciseId) => `${PROGRAMMING_EXERCISES}/${exerciseId}`;
export const SCA_CATEGORIES = (exerciseId) => `/programming-exercises/${exerciseId}/static-code-analysis-categories`;
export const QUIZ_EXERCISES = '/quiz-exercises';
export const QUIZ_EXERCISE = (exerciseId) => `${QUIZ_EXERCISES}/${exerciseId}`;
export const ADMIN_COURSES = '/admin/courses';
export const COURSES = '/courses';
export const USERS = '/users';
export const COURSE = (courseId) => `${COURSES}/${courseId}`;
export const COURSE_STUDENTS = (courseId, username) => `${COURSES}/${courseId}/students/${username}`;
export const COURSE_TUTORS = (courseId, username) => `${COURSES}/${courseId}/tutors/${username}`;
export const COURSE_INSTRUCTORS = (courseId, username) => `${COURSES}/${courseId}/instructors/${username}`;
export const EXERCISES = (courseId) => `${COURSE(courseId)}/exercises`;
export const PARTICIPATION = (exerciseId) => `/exercises/${exerciseId}/participation`;
export const PARTICIPATIONS = (exerciseId) => `/exercises/${exerciseId}/participations`;
export const FILES = (participationId) => `/repository/${participationId}/files`;
export const COMMIT = (participationId) => `/repository/${participationId}/commit`;
export const NEW_FILE = (participationId) => `/repository/${participationId}/file`;
export const PARTICIPATION_WITH_RESULT = (participationId) => `/participations/${participationId}/withLatestResult`;
export const SUBMIT_QUIZ_LIVE = (exerciseId) => `/exercises/${exerciseId}/submissions/live`;
export const SUBMIT_QUIZ_EXAM = (exerciseId) => `/exercises/${exerciseId}/submissions/exam`;
export const EXAMS = (courseId) => `${COURSE(courseId)}/exams`;
export const EXAM = (courseId, examId) => EXAMS(courseId) + `/${examId}`;
export const EXERCISE_GROUPS = (courseId, examId) => `${EXAM(courseId, examId)}/exerciseGroups`;
export const TEXT_EXERCISES = '/text-exercises';
export const TEXT_EXERCISE = (exerciseId) => `/text-exercises/${exerciseId}`;
export const SUBMIT_TEXT_EXAM = (exerciseId) => `/exercises/${exerciseId}/text-submissions`;
export const TEXT_SUBMISSION_WITHOUT_ASSESSMENT = (exerciseId) => `/exercises/${exerciseId}/text-submission-without-assessment?lock=true`;
export const ASSESS_TEXT_SUBMISSION = (exerciseId, resultId) => `/exercise/${exerciseId}/result/${resultId}`;
export const EXAM_STUDENTS = (courseId, examId, username) => `${EXAM(courseId, examId)}/students/${username}`;
export const GENERATE_STUDENT_EXAMS = (courseId, examId) => `${EXAM(courseId, examId)}/generate-student-exams`;
export const STUDENT_EXAMS = (courseId, examId) => `${EXAM(courseId, examId)}/student-exams`;
export const STUDENT_EXAM_WORKINGTIME = (courseId, examId, studentExamId) => `${EXAM(courseId, examId)}/student-exams/${studentExamId}/working-time`;
export const START_EXERCISES = (courseId, examId) => `${EXAM(courseId, examId)}/student-exams/start-exercises`;
export const EVALUATE_QUIZ_EXAM = (courseId, examId) => `${EXAM(courseId, examId)}/student-exams/evaluate-quiz-exercises`;
export const SUBMIT_EXAM = (courseId, examId) => `${EXAM(courseId, examId)}/student-exams/submit`;
export const EXAM_START = (courseId, examId) => `${EXAM(courseId, examId)}/start`;
export const EXAM_CONDUCTION = (courseId, examId, studentExamId) => `${EXAM(courseId, examId)}/student-exams/${studentExamId}/conduction`;
export const MODELING_EXERCISES = '/modeling-exercises';
export const MODELING_EXERCISE = (exerciseId) => `/modeling-exercises/${exerciseId}`;
export const SUBMIT_MODELING_EXAM = (exerciseId) => `/exercises/${exerciseId}/modeling-submissions`;
export const TUTOR_PARTICIPATIONS = (exerciseId) => `/exercises/${exerciseId}/tutor-participations`;
export const MODELING_SUBMISSION_WITHOUT_ASSESSMENT = (exerciseId) => `/exercises/${exerciseId}/modeling-submission-without-assessment?lock=true`;
export const ASSESS_MODELING_SUBMISSION = (submissionId, resultId) => `/modeling-submissions/${submissionId}/result/${resultId}/assessment?submit=true`;
