package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

public interface BehaviorHandler<A, R, T extends Behavior.Compound<A, R>> {
    BehaviorList<A, R> handle(T behaviour);

    PenaltyInfo penaltyInfo();

    record PenaltyInfo(String id, double scale, double timeScale) {
    }

    interface BehaviorList<A, R> {
        Node<A, ?> first();


        Node<?, R> last();

        int size();

        interface Node<A, R> {
            boolean first();

            boolean last();

            int index();

            Behavior<A, R> behavior();

            Behavior<?, A> prev();

            Behavior<R, ?> next();
        }
    }
}
