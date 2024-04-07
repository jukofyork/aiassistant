package eclipse.plugin.aiassistant.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eclipse.plugin.aiassistant.prompt.Prompts;
import eclipse.plugin.aiassistant.view.MainView;

/**
 * This abstract class represents a handler for prompts in a chat conversation within the Eclipse plugin.
 * It provides a base implementation for handling different types of prompts.
 */
public abstract class AbstractPromptHandler extends AbstractHandler {

    private final Prompts type;

    /**
     * Constructs a new AbstractPromptHandler with the specified prompt type.
     *
     * @param type The type of prompt this handler is associated with.
     */
    public AbstractPromptHandler(Prompts type) {
        this.type = type;
    }

    /**
     * Executes the handler by sending a predefined prompt to the main view.
     *
     * @param event The execution event.
     * @return null
     * @throws ExecutionException if an error occurs during execution.
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        MainView.findMainView().ifPresent(mainView -> {
            mainView.getMainPresenter().sendPredefinedPrompt(type);
        });
        return null;
    }

}