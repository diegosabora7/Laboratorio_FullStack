import { useState, useEffect } from 'react';
import { taskApi } from '../../api/client';

// TODO: [LABORATORIO - BUG 7] - Memory leak: polling sin cleanup. El setInterval NO se limpia al desmontar el componente.
// Cuando el usuario navega a otra página, el intervalo sigue ejecutándose en segundo plano,
// causando actualizaciones de estado en un componente desmontado y warnings en consola.

const ProjectDashboard = () => {
  const [stats, setStats] = useState({
    total: 0,
    todo: 0,
    inProgress: 0,
    inReview: 0,
    done: 0,
  });

  // TODO: [LABORATORIO - BUG 7] - Memory leak: el useEffect no retorna función de cleanup para clearInterval.
  useEffect(() => {
    const fetchStats = async () => {
      console.log('[ProjectDashboard] 🔄 Fetching stats... (polling activo)');
      try {
        const response = await taskApi.get('/api/tasks/project/1/stats');
        console.log('[ProjectDashboard] ✅ Stats recibidos:', response.data);
        if (response.data) {
          setStats({
            total: response.data.totalTasks || 0,
            todo: response.data.todoCount || 0,
            inProgress: response.data.inProgressCount || 0,
            inReview: response.data.inReviewCount || 0,
            done: response.data.doneCount || 0,
          });
        }
      } catch (error) {
        console.warn('[ProjectDashboard] ⚠️ Error fetching stats (componente posiblemente desmontado):', error);
        setStats((prev) => ({ ...prev }));
      }
    };

    console.log('[ProjectDashboard] 🚀 Componente montado - iniciando polling cada 5s');
    fetchStats();

    // TODO: [LABORATORIO - BUG 7] - Este intervalo sigue corriendo incluso después de que el componente se desmonta.
    const interval = setInterval(() => {
      fetchStats();
    }, 5000);

    // BUG: No hay cleanup. El return está vacío.
    // Debería ser: return () => { console.log('[ProjectDashboard] Cleanup: clearInterval'); clearInterval(interval); };
    return()=>{
      console.log('Componente desmontado....');
      clearInterval(interval);
    }; 
  }, []);

  return (
    <div className="dashboard-container" role="region" aria-label="Dashboard de estadísticas del proyecto">
      <h1>📊 Project Dashboard</h1>

      <div className="dashboard-banner" role="alert">
        <h2>🚧 En Construcción</h2>
        <p>Las estadísticas completas están siendo desarrolladas.</p>
      </div>

      <div className="dashboard-stats" aria-label="Estadísticas de tareas">
        <div className="stat-card" aria-label={`Total tareas: ${stats.total}`}>
          <h3>Total Tareas</h3>
          <p className="stat-number">{stats.total}</p>
        </div>
        <div className="stat-card stat-todo" aria-label={`Tareas por hacer: ${stats.todo}`}>
          <h3>📝 TODO</h3>
          <p className="stat-number">{stats.todo}</p>
        </div>
        <div className="stat-card stat-progress" aria-label={`Tareas en progreso: ${stats.inProgress}`}>
          <h3>🔄 En Progreso</h3>
          <p className="stat-number">{stats.inProgress}</p>
        </div>
        <div className="stat-card stat-review" aria-label={`Tareas en revisión: ${stats.inReview}`}>
          <h3>👀 En Revisión</h3>
          <p className="stat-number">{stats.inReview}</p>
        </div>
        <div className="stat-card stat-done" aria-label={`Tareas completadas: ${stats.done}`}>
          <h3>✅ Completado</h3>
          <p className="stat-number">{stats.done}</p>
        </div>
      </div>
    </div>
  );
};

export default ProjectDashboard;
