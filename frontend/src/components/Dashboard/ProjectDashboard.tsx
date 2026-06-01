//import { useState, useEffect } from 'react';
//import { taskApi } from '../../api/client';
import { useProjectStats, useOverdueTasks } from '../../hooks/useProjectStats';

// TODO: [LABORATORIO - BUG 7] - Memory leak: polling sin cleanup. El setInterval NO se limpia al desmontar el componente.
// Cuando el usuario navega a otra página, el intervalo sigue ejecutándose en segundo plano,
// causando actualizaciones de estado en un componente desmontado y warnings en consola.

const ProjectDashboard = () => {
  const { data: stats, isLoading } = useProjectStats(1);
  const { data: overdueTasks = [] } = useOverdueTasks(1);

  if (isLoading || !stats) {
    return <div role="status" aria-live="polite">Cargando estadísticas...</div>;
  }

  // TODO: [LABORATORIO - BUG 7] - Memory leak: el useEffect no retorna función de cleanup para clearInterval.
  
  /*useEffect(() => {
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
  */

return (
    <div className="dashboard-container" role="region" aria-label="Dashboard de estadísticas">
      <h1>📊 Project Dashboard</h1>

      {/* 1. Barra de progreso */}
      <section aria-label="Progreso del proyecto">
        <h2>Progreso General</h2>
        <div className="progress-bar-container">
          <div 
            className="progress-bar-fill"
            style={{ width: `${stats.completionPercentage}%` }}
            role="progressbar"
            aria-valuenow={stats.completionPercentage}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            {stats.completionPercentage.toFixed(0)}%
          </div>
        </div>
      </section>

      {/* 2. Contadores por status */}
      <section aria-label="Contadores por estado">
        <div className="stats-grid">
          <div className="stat-card" style={{ borderLeft: '4px solid #6b7280' }}>
            <h3>📝 TODO</h3>
            <p className="stat-number">{stats.todoCount}</p>
          </div>
          <div className="stat-card" style={{ borderLeft: '4px solid #3b82f6' }}>
            <h3>🔄 En Progreso</h3>
            <p className="stat-number">{stats.inProgressCount}</p>
          </div>
          <div className="stat-card" style={{ borderLeft: '4px solid #f59e0b' }}>
            <h3>👀 En Revisión</h3>
            <p className="stat-number">{stats.inReviewCount}</p>
          </div>
          <div className="stat-card" style={{ borderLeft: '4px solid #10b981' }}>
            <h3>✅ Completado</h3>
            <p className="stat-number">{stats.doneCount}</p>
          </div>
        </div>
      </section>

      {/* 3. Gráfico de distribución (barra horizontal segmentada) */}
      <section aria-label="Distribución de tareas">
        <h2>Distribución</h2>
        <div className="distribution-bar">
          {stats.todoCount > 0 && (
            <div style={{ width: `${(stats.todoCount / stats.totalTasks) * 100}%`, backgroundColor: '#6b7280' }}
                 title={`TODO: ${stats.todoCount}`} />
          )}
          {stats.inProgressCount > 0 && (
            <div style={{ width: `${(stats.inProgressCount / stats.totalTasks) * 100}%`, backgroundColor: '#3b82f6' }}
                 title={`En Progreso: ${stats.inProgressCount}`} />
          )}
          {stats.inReviewCount > 0 && (
            <div style={{ width: `${(stats.inReviewCount / stats.totalTasks) * 100}%`, backgroundColor: '#f59e0b' }}
                 title={`En Revisión: ${stats.inReviewCount}`} />
          )}
          {stats.doneCount > 0 && (
            <div style={{ width: `${(stats.doneCount / stats.totalTasks) * 100}%`, backgroundColor: '#10b981' }}
                 title={`Completado: ${stats.doneCount}`} />
          )}
        </div>
      </section>

      {/* 4. Lista de tareas overdue */}
      <section aria-label="Tareas vencidas">
        <h2>⚠️ Tareas Vencidas ({overdueTasks.length})</h2>
        {overdueTasks.length === 0 ? (
          <p>No hay tareas vencidas 🎉</p>
        ) : (
          <ul className="overdue-list">
            {overdueTasks.map(task => (
              <li key={task.id} className="overdue-item">
                <span className="overdue-title">{task.title}</span>
                <span className="overdue-date">📅 {task.dueDate}</span>
                <span className="overdue-assignee">👤 {task.assigneeName || 'Sin asignar'}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
};  

export default ProjectDashboard;
