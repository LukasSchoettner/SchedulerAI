import React from "react";
import "./CompletionPromptModal.css";

export default function CompletionPromptModal({ task, onComplete, onKeepScheduling, onClose }) {
    if (!task) return null;

    const duePassed =
        task.dueDate && new Date(task.dueDate).getTime() < Date.now();

    const reason = duePassed
        ? "The due date for this task has passed."
        : "The estimated time for this task has been used.";

    return (
        <div className="completion-modal-backdrop">
            <div className="completion-modal">
                <h2>Did you complete this task?</h2>
                <p><strong>{task.title}</strong></p>
                <p>{reason}</p>
                {task.description && (
                    <p className="completion-modal-description">{task.description}</p>
                )}

                <div className="completion-modal-actions">
                    <button onClick={() => onComplete(task)} className="btn-primary">
                        Yes, mark as completed
                    </button>
                    <button onClick={() => onKeepScheduling(task)} className="btn-secondary">
                        No, keep scheduling
                    </button>
                    <button onClick={onClose} className="btn-link">
                        Remind me later
                    </button>
                </div>
            </div>
        </div>
    );
}
