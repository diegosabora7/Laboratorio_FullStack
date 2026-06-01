import { Droppable } from '@hello-pangea/dnd';
import { Task, TaskStatus } from '../../types';
import { canTransition } from '../../utils/transitions';
import TaskCard from './TaskCard';

interface KanbanColumnProps {
  columnId: TaskStatus;
  title: string;
  tasks: Task[];
  draggingFromStatus: TaskStatus | null;
}

// CÓDIGO ANTERIOR (antes de Feature 10):
// const KanbanColumn = ({ columnId, title, tasks }: KanbanColumnProps) => {
//   return (
//     <div className="kanban-column">
//       <Droppable droppableId={columnId}>
//         {(provided, _snapshot) => ( <div ref={provided.innerRef} {...provided.droppableProps}> ... )}
//       </Droppable>
//     </div>
//   );
// };

// FEATURE 10: Se agregó draggingFromStatus prop para resaltar columnas válidas/inválidas
// durante el drag & drop. Las columnas válidas se muestran en verde, las inválidas en gris.
const KanbanColumn = ({ columnId, title, tasks, draggingFromStatus }: KanbanColumnProps) => {
  // Determinar si esta columna es un destino válido durante el drag
  const isValidTarget = draggingFromStatus
    ? canTransition(draggingFromStatus, columnId)
    : null;
  const isSourceColumn = draggingFromStatus === columnId;

  // Clases CSS dinámicas
  const getColumnClass = () => {
    if (!draggingFromStatus || isSourceColumn) return 'kanban-column';
    if (isValidTarget) return 'kanban-column column-valid-target';
    return 'kanban-column column-invalid-target';
  };

  return (
    <div className={getColumnClass()} role="region" aria-label={`Columna ${title}`}>
      <div className="column-header">
        <h2 id={`column-${columnId}`}>{title}</h2>
        <span className="task-count" aria-label={`${tasks.length} tareas`}>{tasks.length}</span>
      </div>

      <Droppable droppableId={columnId} isDropDisabled={draggingFromStatus !== null && !isValidTarget && !isSourceColumn}>
        {(provided, snapshot) => (
          <div
            className={`column-content ${snapshot.isDraggingOver && isValidTarget ? 'dragging-over-valid' : ''}`}
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
