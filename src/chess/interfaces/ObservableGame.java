package chess.interfaces;

/**
 * Defines observer registration for game state changes.
 * <p>
 * This interface exists so views, controllers, or loggers can react to model
 * updates without the model depending on those concrete classes. That keeps the
 * board model MVC compatible and follows the Dependency Inversion Principle.
 */
public interface ObservableGame {

    /**
     * Registers an observer to run when the game state changes.
     *
     * @param observer callback to register
     */
    void addObserver(Runnable observer);

    /**
     * Removes a previously registered observer.
     *
     * @param observer callback to remove
     */
    void removeObserver(Runnable observer);

    /**
     * Notifies all registered observers that the game state changed.
     */
    void notifyObservers();
}
