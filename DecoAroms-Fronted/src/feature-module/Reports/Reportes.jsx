import React, { useState } from "react";
import Breadcrumbs from "../../core/breadcrumbs";
import ResumenGeneralTab from "./ResumenGeneralTab";
import AnalisisVentasTab from "./AnalisisVentasTab";
import AnalisisProductosTab from "./AnalisisProductosTab";
import OperacionesTab from "./OperacionesTab";
import FiltrosGlobales from "./FiltrosGlobales";
import AnalisisClientesTab from "./AnalisisClientesTab";

// Estado inicial por defecto para los filtros
const getDefaultFilters = () => ({
  anio: new Date().getFullYear().toString(),
  mes: "",
  fechaInicio: "",
  fechaFin: "",
  tipoVenta: "",
  tipoCliente: "",
  familiaId: "",
  aromaId: "",
  diasInactividad: "90",
});

const Reportes = () => {
  const [activeTab, setActiveTab] = useState("resumen");

  // --- LÓGICA DE FILTROS ACTUALIZADA ---

  // 'activeFilters' son los filtros que los gráficos están mostrando actualmente.
  const [activeFilters, setActiveFilters] = useState(getDefaultFilters());

  // 'pendingFilters' son los filtros que el usuario está cambiando en el formulario.
  const [pendingFilters, setPendingFilters] = useState(activeFilters);

  // Esta función SÓLO actualiza el estado del formulario, no dispara peticiones.
  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setPendingFilters((prev) => ({ ...prev, [name]: value }));
  };

  // Esta función se llama al presionar "Aplicar".
  // Actualiza los filtros activos, lo que disparará la recarga de los gráficos.
  const handleApplyFilters = () => {
    setActiveFilters(pendingFilters);
  };

  // Esta función limpia el formulario y aplica los filtros limpios.
  const handleClearFilters = () => {
    const defaultFilters = getDefaultFilters();
    setPendingFilters(defaultFilters);
    setActiveFilters(defaultFilters);
  };


  const renderActiveTab = () => {
    const commonProps = { filters: activeFilters };

    switch (activeTab) {
      case "resumen":
        return <ResumenGeneralTab {...commonProps} />;
      case "ventas":
        return <AnalisisVentasTab {...commonProps} />;
      case "productos":
        return <AnalisisProductosTab {...commonProps} />;
      case "operaciones":
        return <OperacionesTab {...commonProps} />;
      case "clientes":
        return <AnalisisClientesTab {...commonProps} />;
      default:
        return <ResumenGeneralTab {...commonProps} />;
    }
  };

  return (
    <div className="page-wrapper">

      <div className="content">
        <div className="page-header">
          <div className="add-item d-flex">
            <div className="page-title">
              <h4>Reportes</h4>
              <h6>Gráficos de Reportes de Negocio</h6>
            </div>
          </div>
        </div>

        <Breadcrumbs maintitle="Módulo de Reportes" subtitle="Visualiza el rendimiento de tu negocio" />

        <FiltrosGlobales
          filters={pendingFilters}
          onFilterChange={handleFilterChange}
          onApplyFilters={handleApplyFilters}
          onClearFilters={handleClearFilters}
          activeTab={activeTab}
        />

        <ul className="nav nav-tabs mt-4">
          <li className="nav-item">
            <a className={`nav-link ${activeTab === 'resumen' ? 'active' : ''}`} href="#" onClick={(e) => { e.preventDefault(); setActiveTab('resumen'); }}>📈 Resumen General</a>
          </li>
          <li className="nav-item">
            <a className={`nav-link ${activeTab === 'ventas' ? 'active' : ''}`} href="#" onClick={(e) => { e.preventDefault(); setActiveTab('ventas'); }}>💰 Análisis de Ventas</a>
          </li>
          <li className="nav-item">
            <a className={`nav-link ${activeTab === 'productos' ? 'active' : ''}`} href="#" onClick={(e) => { e.preventDefault(); setActiveTab('productos'); }}>📦 Análisis de Productos</a>
          </li>
          <li className="nav-item">
            <a className={`nav-link ${activeTab === 'operaciones' ? 'active' : ''}`} href="#" onClick={(e) => { e.preventDefault(); setActiveTab('operaciones'); }}>⚙️ Análisis de Operaciones</a>
          </li>
          <li className="nav-item">
            <a className={`nav-link ${activeTab === 'clientes' ? 'active' : ''}`} href="#" onClick={(e) => { e.preventDefault(); setActiveTab('clientes'); }}>👥 Análisis de Clientes</a>
          </li>
        </ul>

        <div className="tab-content pt-4">
          {renderActiveTab()}
        </div>
      </div>
    </div>
  );
};

export default Reportes;