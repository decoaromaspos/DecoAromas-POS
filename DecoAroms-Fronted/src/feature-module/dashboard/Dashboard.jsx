import React, { useState, useEffect } from "react";
import CountUp from "react-countup";
import {
  User,
  UserCheck,
  Package,
  ShoppingBag,
  DollarSign,
  TrendingUp,
  ArrowRight,
  List,
} from "feather-icons-react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import configuracionService from "../../services/configuracionService";
import { getGananciasDelMesActual, getGananciasDelDiaActual } from '../../services/ventaService';
import { getcantidadClientesActivos } from "../../services/clienteService";
import { getStockGeneral } from "../../services/productoService";
import { all_routes } from "../../Router/all_routes";


const Dashboard = () => {
  const { usuario } = useAuth();
  const adminName = usuario.nombreCompleto;
  const route = all_routes;
  const isSuperAdmin = usuario && usuario.rol === 'SUPER_ADMIN';

  // --- ESTADO PARA LA META Y DATOS DINÁMICOS ---
  const [monthlyGoal, setMonthlyGoal] = useState({
    target: 0, // Valor inicial, se cargará desde la API
    currentProfit: 0, // Valor inicial, se cargará desde la API
  });

  // --- ESTADO PARA LAS MÉTRICAS PRINCIPALES ---
  const [dashboardMetrics, setDashboardMetrics] = useState({
    ventasHoy: 0,
    stockGeneral: 0,
    clientesActivos: 0,
  });


  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);


  // --- USEEFFECT PARA CARGAR DATOS AL MONTAR EL COMPONENTE ---
  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Hacemos todas las llamadas a la API en paralelo para mayor eficiencia
        const [
          metaResponse,
          gananciaMesResponse,
          gananciaDiaResponse,
          clientesActivosResponse,
          stockGeneralResponse,
        ] = await Promise.all([
          configuracionService.getConfiguracion('META_MENSUAL'),
          getGananciasDelMesActual(),
          getGananciasDelDiaActual(), // Nueva llamada
          getcantidadClientesActivos(), // Nueva llamada
          getStockGeneral(),            // Nueva llamada
        ]);

        // Procesamos los datos de la meta
        const target = parseFloat(metaResponse) || 0;
        const currentProfit = parseFloat(gananciaMesResponse) || 0;
        setMonthlyGoal({ target, currentProfit });

        // Procesamos y guardamos las otras métricas
        setDashboardMetrics({
          ventasHoy: parseFloat(gananciaDiaResponse) || 0,
          stockGeneral: parseInt(stockGeneralResponse.stockGeneral, 10) || 0,
          clientesActivos: parseInt(clientesActivosResponse.cantidadClientesActivos, 10) || 0,
        });

      } catch (err) {
        console.error("Error al cargar los datos del dashboard:", err);
        setError("No se pudieron cargar todos los datos. Intente de nuevo más tarde.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboardData();
  }, []);


  const options = {
    style: 'currency',
    currency: 'CLP',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  };

  // --- CÁLCULOS DERIVADOS DEL ESTADO ---
  const goalProgress = monthlyGoal.target > 0 ? (monthlyGoal.currentProfit / monthlyGoal.target) * 100 : 0;
  const progressBarColor = goalProgress >= 100 ? "bg-success" : "bg-warning";

  const primaryMetrics = [
    {
      title: "Ventas de Hoy",
      value: dashboardMetrics.ventasHoy,
      prefix: "$",
      icon: <DollarSign />,
      className: "dash-widget dash1",
    },
    {
      title: "Ventas del Mes",
      value: monthlyGoal.currentProfit,
      prefix: "$",
      icon: <DollarSign />,
      className: "dash-widget dash1",
    },
    {
      title: "Productos en Stock",
      value: dashboardMetrics.stockGeneral,
      prefix: "",
      icon: <Package />,
      className: "dash-widget dash2",
    },
    {
      title: "Clientes Activos",
      value: dashboardMetrics.clientesActivos,
      prefix: "",
      icon: <User />,
      className: "dash-widget dash3",
    },
  ];

  const quickLinks = [
    isSuperAdmin ?
      {
        // --- BOTÓN PARA SUPER_ADMIN ---
        title: "Gestión de Usuarios",
        description: "Administrar roles y accesos",
        icon: <User size={24} />,
        route: route.usuariolist,
        color: "bg-primary", 
      } :
      {
        // --- BOTÓN ORIGINAL POS OTROS ROLES ---
        title: "Punto de Venta",
        description: "Iniciar una nueva venta rápida",
        icon: <ShoppingBag size={24} />,
        route: route.pos,
        color: "bg-primary",
      },
    {
      title: "Lista de Productos",
      description: "Gestionar el inventario de artículos",
      icon: <Package size={24} />,
      route: route.productlist,
      color: "bg-success",
    },
    {
      title: "Clientes",
      description: "Ver y editar la base de clientes",
      icon: <UserCheck size={24} />,
      route: route.customers,
      color: "bg-warning",
    },
    {
      title: "Ventas Realizadas",
      description: "Revisar el historial de transacciones",
      icon: <List size={24} />,
      route: route.saleslist,
      color: "bg-info",
    },
  ];

  // Array of motivational lines
  const motivationalLines = [
    "¡Sigue así, tú puedes!",
    "¡El éxito está a la vuelta de la esquina!",
    "¡Cada paso cuenta, sigue avanzando!",
    "¡Tu esfuerzo dará frutos, no te rindas!",
    "¡Eres capaz de lograr grandes cosas!",
    "¡Confía en ti y sigue adelante!",
    "¡Hoy es un gran día para alcanzar tus metas!",
    "¡La perseverancia es la clave del éxito!",
    
  ];

  // Select a random motivational line
  const randomMotivationalLine = motivationalLines[Math.floor(Math.random() * motivationalLines.length)];

  const renderGoalCardContent = () => {
    if (isLoading) {
      return <p>Cargando datos de la meta...</p>;
    }

    if (error) {
      return <p className="text-danger">{error}</p>;
    }

    return (
      <>
        <div className="d-flex justify-content-between mb-2">
          <h5 className="mb-0">Venta Actual: <strong>{monthlyGoal.currentProfit.toLocaleString('es-CL', options)}</strong></h5>
          <h5 className="mb-0">Meta: <strong>{monthlyGoal.target.toLocaleString('es-CL', options)}</strong></h5>
        </div>
        <div className="progress mb-2" style={{ height: "25px" }}>
          <div
            className={`progress-bar ${progressBarColor} progress-bar-striped progress-bar-animated`}
            role="progressbar"
            style={{ width: `${Math.min(goalProgress, 100)}%` }}
            aria-valuenow={Math.min(goalProgress, 100)}
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <strong>{Math.round(goalProgress)}% completado</strong>
          </div>
        </div>
        {goalProgress >= 100 ? (
          <p className="text-success fw-bold mt-2">¡Felicidades! Meta mensual superada.</p>
        ) : (
          <p className="mt-2" style={{ color: "#6c757d" }}>
            Faltan <strong>{(monthlyGoal.target - monthlyGoal.currentProfit).toLocaleString('es-CL', options)}</strong> para alcanzar la meta. {randomMotivationalLine}
          </p>
        )}
      </>
    );
  };


  return (
    <div>
      <div className="page-wrapper">
        <div className="content">
          <div className="page-header">
            <div className="row">
              <div className="col-sm-12">
                <h1 className="page-title">
                  👋 ¡Bienvenido de vuelta, {adminName}!
                </h1>
                <p className="lead">Vista rápida de tus operaciones de hoy.</p>
              </div>
            </div>
          </div>

          <div className="row">
            {isLoading ? (
              <p>Cargando métricas...</p>
            ) : (
              primaryMetrics.map((metric, index) => (
                <div key={index} className="col-xl-3 col-sm-6 col-12 d-flex">
                  <div className={`${metric.className} w-100`}>
                    <div className="dash-widgetimg">
                      <span className={metric.color}>{metric.icon}</span>
                    </div>
                    <div className="dash-widgetcontent">
                      <h5>
                        {metric.prefix}
                        <CountUp
                          start={0}
                          end={metric.value}
                          duration={2}
                          decimals={0}
                          separator="."
                          decimal=","
                        />
                      </h5>
                      <h6>{metric.title}</h6>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="row mt-4">
            <div className="col-xl-12">
              <div className="card default-cover">
                <div className="card-header">
                  <h4 className="card-title mb-0 d-flex align-items-center">
                    <TrendingUp className="me-2" size={20} />
                    Meta de Venta Mensual
                  </h4>
                </div>
                <div className="card-body">
                  {renderGoalCardContent()}
                </div>
              </div>
            </div>
          </div>

          <h4 className="card-title mb-3 mt-4">Accesos Directos Clave ⚡</h4>
          <div className="row">
            {quickLinks.map((link, index) => (
              <div key={index} className="col-xl-3 col-sm-6 col-12 d-flex">
                <Link to={link.route} className="card flex-fill mb-4 w-100 text-decoration-none">
                  <div className={`card-body text-center ${link.color} text-white rounded shadow`}>
                    <div className="mb-2">{link.icon}</div>
                    <h5 className="card-title text-white">{link.title}</h5>
                    <p className="card-text text-white-50">{link.description}</p>
                    <ArrowRight size={16} />
                  </div>
                </Link>
              </div>
            ))}
          </div>

        </div>
      </div>
    </div>
  );
};

export default Dashboard;