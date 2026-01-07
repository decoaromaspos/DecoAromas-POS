import React from "react";
import PropTypes from 'prop-types';
import ReactApexChart from "react-apexcharts";

const ChartRenderer = ({ loading, options, series, type, message, height }) => {
  if (loading) {
    return <div className="text-center p-5">Cargando datos...</div>;
  }
  
  const hasNoData = () => {
    if (!series || series.length === 0) {
      return true;
    }
    if (typeof series[0] === 'object' && series[0] !== null && Object.prototype.hasOwnProperty.call(series[0], 'data')) {
      return series.every(s => !s.data || s.data.length === 0);
    }
    if (series.every(val => typeof val === 'number')) {
      const total = series.reduce((sum, current) => sum + current, 0);
      return total === 0;
    }
    return false;
  };
  
  const noData = hasNoData();
  
  if (noData) {
    return <div className="text-center p-5 fst-italic">{message}</div>;
  }

  return <ReactApexChart options={options} series={series} type={type} height={height} />;
};

ChartRenderer.propTypes = {
  loading: PropTypes.bool.isRequired,
  options: PropTypes.object.isRequired,
  series: PropTypes.array.isRequired,
  type: PropTypes.string.isRequired,
  message: PropTypes.string,
  height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

ChartRenderer.defaultProps = {
  message: "No hay datos para mostrar.",
  height: 350,
};


export default ChartRenderer;