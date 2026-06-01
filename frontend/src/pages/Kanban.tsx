import { useState } from 'react';
import { KanbanBoard } from '../components/KanbanBoard';
import SearchBar, { SearchFiltersState } from '../components/TaskFilters/SearchBar';
import { useTaskSearch } from '../hooks/useTaskSearch';
import '../styles/Kanban.css';

// CÓDIGO ANTERIOR:
// const Kanban = () => {
//   const [projectId] = useState(1);
//   return (
//     <div className="kanban-container">
//       <div className="kanban-header"><h1>Tablero Kanban</h1><SearchBar /></div>
//       <KanbanBoard projectId={projectId} />
//     </div>
//   );
// };

// FEATURE 8: Integración de búsqueda avanzada con el Kanban
const Kanban = () => {
  const [projectId] = useState(1);
  const [filters, setFilters] = useState<SearchFiltersState>({
    status: '', priority: '', assigneeId: '', query: ''
  });

  const hasActiveFilters = Object.values(filters).some(v => v !== '');

  const { data: searchResults } = useTaskSearch({
    status: filters.status || undefined,
    priority: filters.priority || undefined,
    assigneeId: filters.assigneeId ? Number(filters.assigneeId) : undefined,
    query: filters.query || undefined,
    projectId: projectId,
  });

  return (
    <div className="kanban-container">
      <div className="kanban-header">
        <h1>Tablero Kanban</h1>
      </div>

      <SearchBar onFiltersChange={setFilters} />

      {hasActiveFilters && searchResults ? (
        <div className="search-results">
          <h2>Resultados de búsqueda ({searchResults.totalElements})</h2>
          <div className="search-results-list">
            {searchResults.content.map(task => (
              <div key={task.id} className="search-result-item">
                <span className="result-title">{task.title}</span>
                <span className="result-status">{task.status}</span>
                <span className="result-priority">{task.priority}</span>
                <span className="result-assignee">{task.assigneeName || 'Sin asignar'}</span>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <KanbanBoard projectId={projectId} />
      )}
    </div>
  );
};

export default Kanban;
