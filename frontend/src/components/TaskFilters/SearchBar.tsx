import { useState } from 'react';

// TODO: [FEATURE 8] Conectar estos combos y el campo de texto con el endpoint
// de búsqueda avanzada GET /api/tasks/search?status=X&priority=Y&assigneeId=Z
// Los selects están disabled hasta que se implemente la lógica.

const SearchBar = () => {
  const [searchText, setSearchText] = useState('');

  return (
    <div className="search-bar">
      <input
        type="text"
        placeholder="Buscar tareas..."
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        className="search-input"
      />

      <select className="search-select" disabled>
        <option value="">Estado</option>
        <option value="TODO">TODO</option>
        <option value="IN_PROGRESS">IN_PROGRESS</option>
        <option value="IN_REVIEW">IN_REVIEW</option>
        <option value="DONE">DONE</option>
      </select>

      <select className="search-select" disabled>
        <option value="">Prioridad</option>
        <option value="LOW">LOW</option>
        <option value="MEDIUM">MEDIUM</option>
        <option value="HIGH">HIGH</option>
        <option value="CRITICAL">CRITICAL</option>
      </select>

      <select className="search-select" disabled>
        <option value="">Asignado</option>
      </select>

      <button className="search-button" disabled>
        Buscar
      </button>
    </div>
  );
};

export default SearchBar;
