import { useState } from 'react';
import { DragDropContext, DropResult } from '@hello-pangea/dnd';
import { useTasksByProject } from '../../hooks/useTasks';
import { useUpdateTaskStatus } from '../../hooks/useTaskMutation';
import { TaskStatus } from '../../types';
import KanbanColumn from './KanbanColumn';

const COLUMNS: { id: TaskStatus; title: string }[] = [
  { id: 'TODO', title: 'Por Hacer' },
  { id: 'IN_PROGRESS', title: 'En Progreso' },
  { id: 'IN_REVIEW', title: 'En Revisión' },
  { id: 'DONE', title: 'Completado' },
];

interface KanbanBoardProps {
  projectId: number;
}

const KanbanBoard = ({ projectId }: KanbanBoardProps) => {
  const { data: tasks = [], isLoading } = useTasksByProject(projectId);
  const updateStatus = useUpdateTaskStatus(projectId);

  const onDragEnd = (result: DropResult) => {
    const { destination, draggableId } = result;

    if (!destination) return;

    const newStatus = destination.droppableId as TaskStatus;
    const taskId = parseInt(draggableId, 10);

    updateStatus.mutate({ taskId, status: newStatus });
  };

  if (isLoading) {
    return (
      <div className="kanban-loading" role="status" aria-live="polite">
        <p>Cargando tareas...</p>
      </div>
    );
  }

  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <div className="kanban-board" role="region" aria-label="Tablero Kanban">
        {COLUMNS.map((column) => (
          <KanbanColumn
            key={column.id}
            columnId={column.id}
            title={column.title}
            tasks={tasks.filter((task) => task.status === column.id)}
          />
        ))}
      </div>
    </DragDropContext>
  );
};

export default KanbanBoard;
