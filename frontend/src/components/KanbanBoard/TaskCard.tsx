import { Draggable } from '@hello-pangea/dnd';
import { Task } from '../../types';

interface TaskCardProps {
  task: Task;
  index: number;
}

const priorityColors: Record<string, string> = {
  LOW: '#4caf50',
  MEDIUM: '#ff9800',
  HIGH: '#f44336',
  CRITICAL: '#9c27b0',
};

const TaskCard = ({ task, index }: TaskCardProps) => {
  return (
    <Draggable draggableId={String(task.id)} index={index}>
      {(provided, snapshot) => (
        <article
          className={`task-card ${snapshot.isDragging ? 'dragging' : ''}`}
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          role="listitem"
          aria-label={`Tarea: ${task.title}, prioridad ${task.priority}, asignada a ${task.assigneeName || 'nadie'}`}
        >
          <div className="task-card-header">
            <span
              className="priority-badge"
              style={{ backgroundColor: priorityColors[task.priority] }}
              aria-label={`Prioridad: ${task.priority}`}
            >
              {task.priority}
            </span>
          </div>
          <h3 className="task-title">{task.title}</h3>
          <p className="task-description">{task.description}</p>
          <div className="task-card-footer">
            <span className="task-assignee">👤 {task.assigneeName || 'Sin asignar'}</span>
          </div>
        </article>
      )}
    </Draggable>
  );
};

export default TaskCard;
