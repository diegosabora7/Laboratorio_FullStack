import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

// CÓDIGO ANTERIOR (placeholder):
// const SearchBar = () => {
//   const [searchText, setSearchText] = useState('');
//   return ( <div className="search-bar"> ... selects disabled ... </div> );
// };

// FEATURE 8: Filtros funcionales con chips, URL sync y Clear all

export interface SearchFiltersState {
  status: string;
  priority: string;
  assigneeId: string;
  query: string;
}

interface SearchBarProps {
  onFiltersChange: (filters: SearchFiltersState) => void;
}

const SearchBar = ({ onFiltersChange }: SearchBarProps) => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Inicializar desde URL params (sincronización con URL)
  const [filters, setFilters] = useState<SearchFiltersState>({
    status: searchParams.get('status') || '',
    priority: searchParams.get('priority') || '',
    assigneeId: searchParams.get('assigneeId') || '',
    query: searchParams.get('query') || '',
  });

  // Sincronizar filtros con URL
  useEffect(() => {
    const params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.priority) params.set('priority', filters.priority);
    if (filters.assigneeId) params.set('assigneeId', filters.assigneeId);
    if (filters.query) params.set('query', filters.query);
    setSearchParams(params, { replace: true });
    onFiltersChange(filters);
  }, [filters]);

  const updateFilter = (key: keyof SearchFiltersState, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const removeFilter = (key: keyof SearchFiltersState) => {
    setFilters(prev => ({ ...prev, [key]: '' }));
  };

  const clearAll = () => {
    setFilters({ status: '', priority: '', assigneeId: '', query: '' });
  };

  // Filtros activos para mostrar como chips
  const activeFilters = Object.entries(filters).filter(([_, value]) => value !== '');

  return (
    <div className="search-filters">
      <div className="search-bar">
        <input
          type="text"
          placeholder="Buscar en título o descripción..."
          value={filters.query}
          onChange={(e) => updateFilter('query', e.target.value)}
          className="search-input"
          aria-label="Buscar tareas"
        />

        <select
          className="search-select"
          value={filters.status}
          onChange={(e) => updateFilter('status', e.target.value)}
          aria-label="Filtrar por estado"
        >
          <option value="">Estado</option>
          <option value="TODO">TODO</option>
          <option value="IN_PROGRESS">IN_PROGRESS</option>
          <option value="IN_REVIEW">IN_REVIEW</option>
          <option value="DONE">DONE</option>
        </select>

        <select
          className="search-select"
          value={filters.priority}
          onChange={(e) => updateFilter('priority', e.target.value)}
          aria-label="Filtrar por prioridad"
        >
          <option value="">Prioridad</option>
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>

        <select
          className="search-select"
          value={filters.assigneeId}
          onChange={(e) => updateFilter('assigneeId', e.target.value)}
          aria-label="Filtrar por asignado"
        >
          <option value="">Asignado</option>
          <option value="1">Usuario 1</option>
          <option value="2">Usuario 2</option>
        </select>
      </div>

      {/* Chips de filtros activos */}
      {activeFilters.length > 0 && (
        <div className="filter-chips" role="list" aria-label="Filtros activos">
          {activeFilters.map(([key, value]) => (
            <span key={key} className="filter-chip" role="listitem">
              {key}: {value}
              <button
                onClick={() => removeFilter(key as keyof SearchFiltersState)}
                className="chip-remove"
                aria-label={`Remover filtro ${key}`}
              >
                ×
              </button>
            </span>
          ))}
          <button onClick={clearAll} className="clear-all-btn">
            Clear all
          </button>
        </div>
      )}
    </div>
  );
};

export default SearchBar;
