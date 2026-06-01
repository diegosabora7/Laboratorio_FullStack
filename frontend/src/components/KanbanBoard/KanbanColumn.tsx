import { Droppable } from '@hello-pangea/dnd';
import { Task, TaskStatus } from '../../types';
import TaskCard from './TaskCard';

interface KanbanColumnProps {
  columnId: TaskStatus;
  title: string;
  tasks: Task[];
}

// TODO: [FEATURE 10] Pintar esta columna de ROJO si la transición es inválida
// o VERDE si es válida durante el Drag & Drop, y mostrar mensaje de error.
// Implementar lógica de validación de transiciones:
// TODO -> IN_PROGRESS (válido)
// IN_PROGRESS -> IN_REVIEW, TODO (válido)
// IN_REVIEW -> DONE, IN_PROGRESS (válido)
// DONE -> ninguno (inválido)

const KanbanColumn = ({ columnId, title, tasks }: KanbanColumnProps) => {
  return (
    <div className="kanban-column" role="region" aria-label={`Columna ${title}`}>
      <div className="column-header">
        <h2 id={`column-${columnId}`}>{title}</h2>
        <span className="task-count" aria-label={`${tasks.length} tareas`}>{tasks.length}</span>
      </div>

      <Droppable droppableId={columnId}>
        {(provided, _snapshot) => (
          <div
            className="column-content"
            ref={provided.innerRef}
            {...provided.droppableProps}
            role="list"
            aria-labelledby={`column-${columnId}`}
          >
            {tasks.map((task, index) => (
              <TaskCard key={task.id} task={task} index={index} />
            ))}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </div>
  );
};

export default KanbanColumn;
