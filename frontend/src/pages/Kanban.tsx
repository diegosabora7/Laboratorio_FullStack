import { useState } from 'react';
import { KanbanBoard } from '../components/KanbanBoard';
import { SearchBar } from '../components/TaskFilters';
import '../styles/Kanban.css';

const Kanban = () => {
  const [projectId] = useState(1);

  return (
    <div className="kanban-container">
      <div className="kanban-header">
        <h1>Tablero Kanban</h1>
        <SearchBar />
      </div>

      <KanbanBoard projectId={projectId} />
    </div>
  );
};

export default Kanban;
