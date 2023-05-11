package io.github.fossure.service.pipeline;

public interface Step<I, O> {
    class StepException extends RuntimeException {

        public StepException(Throwable t) {
            super(t);
        }
    }

    O process(I input) throws StepException;
}
