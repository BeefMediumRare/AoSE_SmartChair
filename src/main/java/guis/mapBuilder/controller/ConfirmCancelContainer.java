package guis.mapBuilder.controller;

import javafx.scene.control.Button;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BooleanSupplier;

class ConfirmCancelContainer {
    private final @Nonnull
    Button confirmButton_;
    private final @Nonnull
    Button cancelButton_;

    private BooleanSupplier wrappedConfirmAction_;
    private BooleanSupplier wrappedCancelAction_;

    ConfirmCancelContainer(@Nonnull Button confirm, @Nonnull Button cancel) {
        confirmButton_ = confirm;
        cancelButton_ = cancel;
    }

    /**
     * Makes the confirm and cancel buttons visible.
     *
     * @param confirmAction the action that will be executed if the confirm button was pressed.
     *                      If the confirm action returns true the confirm and cancel button will disappear.
     * @param cancelAction  the action that will be executed if the cancel button was pressed.
     *                      If the cancel action returns true the confirm and cancel button will disappear.
     */
    void showButtons(@Nonnull BooleanSupplier confirmAction, @Nonnull BooleanSupplier cancelAction) {
        confirmButton_.setVisible(true);
        cancelButton_.setVisible(true);

        wrappedConfirmAction_ = wrap(confirmAction);
        confirmButton_.setOnMousePressed(e -> wrappedConfirmAction_.getAsBoolean());

        wrappedCancelAction_ = wrap(cancelAction);
        cancelButton_.setOnMousePressed(e -> wrappedCancelAction_.getAsBoolean());
    }

    private BooleanSupplier wrap(@Nonnull BooleanSupplier supplier) {
        return () -> {
            boolean result = supplier.getAsBoolean();
            if (result) {
                confirmButton_.setVisible(false);
                cancelButton_.setVisible(false);

                confirmButton_.setOnMousePressed(e -> {
                });
                cancelButton_.setOnMousePressed(e -> {
                });

                wrappedConfirmAction_ = null;
                wrappedCancelAction_ = null;
            }
            return result;
        };
    }

    @Nonnull
    Optional<Boolean> confirm() {
        return Optional.ofNullable(wrappedConfirmAction_ != null ? wrappedConfirmAction_.getAsBoolean() : null);
    }

    @Nonnull
    Optional<Boolean> cancel() {
        return Optional.ofNullable(wrappedCancelAction_ != null ? wrappedCancelAction_.getAsBoolean() : null);
    }
}