export const formatCurrency = (value) => {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    minimumFractionDigits: 0,
  }).format(value || 0);
};

export const getDynamicTitles = (baseTitle, anio, mes) => {
  const mesOptions = [
    { value: "1", label: "Enero" }, { value: "2", label: "Febrero" },
    { value: "3", label: "Marzo" }, { value: "4", label: "Abril" },
    { value: "5", label: "Mayo" }, { value: "6", label: "Junio" },
    { value: "7", label: "Julio" }, { value: "8", label: "Agosto" },
    { value: "9", label: "Septiembre" }, { value: "10", label: "Octubre" },
    { value: "11", label: "Noviembre" }, { value: "12", label: "Diciembre" },
  ];

  let subtitleText = anio || '';
  if (mes && mes !== "") {
    subtitleText += ` - ${mesOptions.find(m => m.value === mes)?.label || ''}`;
  }

  return {
    text: baseTitle,
    align: 'center',
    style: { fontSize: '16px' },
    subtitle: {
      text: subtitleText,
      align: 'center',
      style: {
        fontSize: '12px',
        color: '#9699a2'
      }
    }
  };
};

export const chartOptions = (title, categories, anio, mes) => ({
  chart: { height: 350, type: 'bar', toolbar: { show: true } },
  plotOptions: { bar: { horizontal: false, columnWidth: '55%', endingShape: 'rounded' } },
  dataLabels: { enabled: false },
  stroke: { show: true, width: 2, colors: ['transparent'] },
  xaxis: { categories, labels: { trim: true, minHeight: 100, maxHeight: 150 } },
  yaxis: { title: { text: 'Valor' } },
  fill: { opacity: 1 },
  title: getDynamicTitles(title, anio, mes),
});

export const horizontalBarOptions = (title, categories, anio, mes, yAxisTitle = '') => ({
  ...chartOptions(title, categories, anio, mes),

  plotOptions: {
    bar: {
      horizontal: true,
      borderRadius: 4,
      distributed: true
    }
  },

  xaxis: { // Eje inferior (Valores)
    ...chartOptions(title, categories, anio, mes).xaxis,
    title: { text: 'Cantidad Vendida' }, // Este es el eje X (valores)
    labels: {
      formatter: (val) => { // Formato de número, no moneda
        if (typeof val === 'number') {
          return val.toLocaleString('es-CL');
        }
        return val;
      }
    }
  },

  yaxis: { // Eje izquierdo (Categorías)
    title: { text: yAxisTitle },
    categories: categories,
  },

  tooltip: {
    x: { // Eje X (Valores)
      formatter: (val) => {
        if (typeof val === 'number') {
          return val.toLocaleString('es-CL') + " unidades";
        }
        return val;
      }
    },
    y: { // Eje Y (Categorías)
      title: {
        formatter: (seriesName) => seriesName
      }
    }
  },

  legend: { show: false }
});

export const pieChartOptions = (title, labels, anio, mes) => ({
  chart: {
    type: 'donut',
    width: '100%',
    height: 420,
  },

  legend: {
    position: 'bottom',
    horizontalAlign: 'center',
    floating: false,
    fontSize: '12px',
    itemMargin: {
      horizontal: 10,
      vertical: 5
    },
  },

  plotOptions: {
    pie: {
      donut: {
        size: '65%',
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Total',
            formatter: function (w) {
              const total = w.globals.seriesTotals.reduce((a, b) => a + b, 0);
              return formatCurrency(total);
            }
          },
          value: {
            show: true,
            formatter: function (val) {
              return formatCurrency(val);
            }
          }
        }
      }
    }
  },

  tooltip: {
    y: {
      formatter: function (val) {
        return formatCurrency(val);
      }
    }
  },

  labels: labels,

  responsive: [{
    breakpoint: 480,
    options: {
      chart: { width: 200 },
      legend: { position: 'bottom' }
    }
  }],

  title: getDynamicTitles(title, anio, mes),
});


export const pieChartCountOptions = (title, labels, anio, mes) => ({
  chart: {
    type: 'donut',
    width: '100%',
    height: 420,
  },

  legend: {
    position: 'bottom',
    horizontalAlign: 'center',
    floating: false,
    fontSize: '12px',
    itemMargin: {
      horizontal: 10,
      vertical: 5
    },
  },

  plotOptions: {
    pie: {
      donut: {
        size: '65%',
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Total',
            // --- CAMBIO CLAVE 1: Mostrar como número simple ---
            formatter: function (w) {
              const total = w.globals.seriesTotals.reduce((a, b) => a + b, 0);
              return total.toLocaleString('es-CL', { minimumFractionDigits: 0 });
            }
          },
          value: {
            show: true,
            // --- CAMBIO CLAVE 2: Mostrar como número simple ---
            formatter: function (val) {
              return val.toLocaleString('es-CL', { minimumFractionDigits: 0 });
            }
          }
        }
      }
    }
  },

  tooltip: {
    y: {
      // --- CAMBIO CLAVE 3: Mostrar como número simple en el tooltip ---
      formatter: function (val) {
        return val.toLocaleString('es-CL', { minimumFractionDigits: 0 });
      }
    }
  },

  labels: labels,

  responsive: [{
    breakpoint: 480,
    options: {
      chart: { width: 200 },
      legend: { position: 'bottom' }
    }
  }],

  title: getDynamicTitles(title, anio, mes),
});