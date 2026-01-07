import React from 'react';

import * as Icon from 'react-feather';
import { all_routes } from "../../Router/all_routes";

const route = all_routes;
const ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN'];
const PUEDE_VENDER = ['VENDEDOR', 'ADMIN'];

export const SidebarData = [

  {
    label: "Principal",
    submenuOpen: true,
    showSubRoute: false,
    submenuHdr: "Main",
    submenuItems: [
      { label: "Inicio", link: route.dashboard, icon: <Icon.Home />, submenu: false, showSubRoute: false, }
    ]
  },
  {
    label: "Inventario",
    submenuOpen: true,
    showSubRoute: false,
    submenuHdr: "Inventory",

    submenuItems: [
      { label: "Productos", link: route.productlist, icon: <Icon.Box />, showSubRoute: false, submenu: false },
      { label: "Crear Producto", link: route.addproduct, icon: <Icon.PlusSquare />, showSubRoute: false, submenu: false, roles: ADMIN_ROLES },
      { label: "Bajo Stock", link: route.lowstock, icon: <Icon.TrendingDown />, showSubRoute: false, submenu: false },
      { label: "Aromas", link: route.categorylist, icon: <Icon.Codepen />, showSubRoute: false, submenu: false },
      { label: "Familias", link: route.familylist, icon: <Icon.Speaker />, showSubRoute: false, submenu: false },
      { label: "Movimientos Inventario", link: route.inventarymovs, icon: <Icon.Inbox />, showSubRoute: false },
      { label: "Imprimir Código Barras", link: "/barcode", icon: <Icon.AlignJustify />, showSubRoute: false, submenu: false }
    ]
  },
  {
    label: "Ventas",
    submenuOpen: true,
    submenuHdr: "Sales",
    submenu: false,
    showSubRoute: false,
    submenuItems: [
      { label: "Ventas", link: route.saleslist, icon: <Icon.ShoppingCart />, showSubRoute: false, submenu: false },
      { label: "POS", link: route.pos, icon: <Icon.HardDrive />, showSubRoute: false, submenu: false, roles: PUEDE_VENDER },
      { label: "Cajas", link: route.cashboxes, icon: <Icon.Archive />, showSubRoute: false, submenu: false },
      { label: "Cotizaciones", link: route.cotizaciones, icon: <Icon.Columns />, showSubRoute: false, submenu: false },
      { label: "Ventas Online", link: route.ventaOnlineList, icon: <Icon.ShoppingBag />, showSubRoute: false, submenu: false }
    ]
  },

  {
    label: "Personas",
    submenuOpen: true,
    showSubRoute: false,
    submenuHdr: "People",

    submenuItems: [
      { label: "Clientes", link: route.customers, icon: <Icon.User />, showSubRoute: false, submenu: false },
      { label: "Usuarios", link: route.usuariolist, icon: <Icon.Users />, showSubRoute: false, submenu: false, roles: ADMIN_ROLES }
    ]
  },
  {
    label: "Reportes",
    submenuOpen: true,
    showSubRoute: false,
    submenuHdr: "Reports",
    submenuItems: [

      // Este podria utilizarse
      { label: "Reportes de Negocio", link: route.reportes, icon: <Icon.TrendingDown />, showSubRoute: false }
    ],
  },
  {
    label: "Configuración",
    submenu: true,
    showSubRoute: false,
    submenuHdr: "Settings",
    submenuItems: [

      { label: "Perfil", link: route.profile, icon: <Icon.User />, showSubRoute: false, roles: PUEDE_VENDER },
      { label: "General", link: route.configuracion, icon: <Icon.Settings />, showSubRoute: false },
      { label: "Cerrar sesión", action: "logout", icon: <Icon.LogOut />, showSubRoute: false }
    ]
  },


  /*
  {
    label: 'UI Interface',
    submenuOpen: true,
    showSubRoute: false,
    submenuHdr: 'UI Interface',
    submenuItems: [
      {
        label: 'Base UI',
        submenu: true,
        showSubRoute: false,
        icon: <Icon.Layers/>,
        submenuItems: [
          { label: 'Alerts', link: '/ui-alerts',showSubRoute: false },
          { label: 'Accordion', link: '/ui-accordion',showSubRoute: false },
          { label: 'Avatar', link: '/ui-avatar',showSubRoute: false },
          { label: 'Badges', link: '/ui-badges',showSubRoute: false },
          { label: 'Border', link: '/ui-borders',showSubRoute: false },
          { label: 'Buttons', link: '/ui-buttons',showSubRoute: false },
          { label: 'Button Group', link: '/ui-buttons-group',showSubRoute: false },
          { label: 'Breadcrumb', link: '/ui-breadcrumb',showSubRoute: false },
          { label: 'Card', link: '/ui-cards',showSubRoute: false },
          { label: 'Carousel', link: '/ui-carousel',showSubRoute: false },
          { label: 'Colors', link: '/ui-colors',showSubRoute: false },
          { label: 'Dropdowns', link: '/ui-dropdowns',showSubRoute: false },
          { label: 'Grid', link: '/ui-grid',showSubRoute: false },
          { label: 'Images', link: '/ui-images',showSubRoute: false },
          { label: 'Lightbox', link: '/ui-lightbox',showSubRoute: false },
          { label: 'Media', link: '/ui-media',showSubRoute: false },
          { label: 'Modals', link: '/ui-modals',showSubRoute: false },
          { label: 'Offcanvas', link: '/ui-offcanvas',showSubRoute: false },
          { label: 'Pagination', link: '/ui-pagination',showSubRoute: false },
          { label: 'Popovers', link: '/ui-popovers',showSubRoute: false },
          { label: 'Progress', link: '/ui-progress',showSubRoute: false },
          { label: 'Placeholders', link: '/ui-placeholders',showSubRoute: false },
          { label: 'Range Slider', link: '/ui-rangeslider',showSubRoute: false },
          { label: 'Spinner', link: '/ui-spinner',showSubRoute: false},
          { label: 'Sweet Alerts', link: '/ui-sweetalerts',showSubRoute: false },
          { label: 'Tabs', link: '/ui-nav-tabs',showSubRoute: false },
          { label: 'Toasts', link: '/ui-toasts',showSubRoute: false },
          { label: 'Tooltips', link: '/ui-tooltips',showSubRoute: false },
          { label: 'Typography', link: '/ui-typography',showSubRoute: false },
          { label: 'Video', link: '/ui-video',showSubRoute: false }
        ]
      },
      {
        label: 'Advanced UI',
        submenu: true,
        showSubRoute: false,
        icon: <Icon.Layers/>,
        submenuItems: [
          { label: 'Ribbon', link: '/ui-ribbon' ,showSubRoute: false},
          { label: 'Clipboard', link: '/ui-clipboard',showSubRoute: false },
          { label: 'Drag & Drop', link: '/ui-drag-drop',showSubRoute: false },
          { label: 'Range Slider', link: '/ui-rangeslider',showSubRoute: false },
          { label: 'Rating', link: '/ui-rating',showSubRoute: false },
          { label: 'Text Editor', link: '/ui-text-editor',showSubRoute: false },
          { label: 'Counter', link: '/ui-counter',showSubRoute: false },
          { label: 'Scrollbar', link: '/ui-scrollbar',showSubRoute: false },
          { label: 'Sticky Note', link: '/ui-stickynote',showSubRoute: false },
          { label: 'Timeline', link: '/ui-timeline',showSubRoute: false }
        ]
      },
      {
        label: 'Charts',
        submenu: true,
        showSubRoute: false,
        icon: <Icon.BarChart2/>,
        submenuItems: [
          { label: 'Apex Charts', link: '/chart-apex',showSubRoute: false },
          { label: 'Chart Js', link: '/chart-js',showSubRoute: false },
        ]
      },
      {
        label: 'Icons',
        submenu: true,
        showSubRoute: false,
        icon: <Icon.Database/>,
        submenuItems: [
          { label: 'Fontawesome Icons', link: '/icon-fontawesome',showSubRoute: false },
          { label: 'Feather Icons', link: '/icon-feather',showSubRoute: false },
          { label: 'Ionic Icons', link: '/icon-ionic',showSubRoute: false },
          { label: 'Material Icons', link: '/icon-material',showSubRoute: false },
          { label: 'Pe7 Icons', link: '/icon-pe7',showSubRoute: false },
          { label: 'Simpleline Icons', link: '/icon-simpleline',showSubRoute: false },
          { label: 'Themify Icons', link: '/icon-themify',showSubRoute: false },
          { label: 'Weather Icons', link: '/icon-weather',showSubRoute: false },
          { label: 'Typicon Icons', link: '/icon-typicon',showSubRoute: false },
          { label: 'Flag Icons', link: '/icon-flag',showSubRoute: false }
        ]
      },
      {
        label: 'Forms',
        submenu: true,
        showSubRoute: false,
        icon: <Icon.Edit/>,
        submenuItems: [
          {
            label: 'Form Elements',
            submenu: true,
            showSubRoute: false,
            submenuItems: [
              { label: 'Basic Inputs', link: '/form-basic-inputs' ,showSubRoute: false},
              { label: 'Checkbox & Radios', link: '/form-checkbox-radios',showSubRoute: false },
              { label: 'Input Groups', link: '/form-input-groups',showSubRoute: false },
              { label: 'Grid & Gutters', link: '/form-grid-gutters',showSubRoute: false },
              { label: 'Form Select', link: '/form-select',showSubRoute: false },
              { label: 'Input Masks', link: '/form-mask',showSubRoute: false },
              { label: 'File Uploads', link: '/form-fileupload',showSubRoute: false }
            ]
          },
          {
            label: 'Layouts',
            submenu: true,
            showSubRoute: false,
            submenuItems: [
              { label: 'Horizontal Form', link: '/form-horizontal' },
              { label: 'Vertical Form', link: '/form-vertical' },
              { label: 'Floating Labels', link: '/form-floating-labels' }
            ]
          },
          { label: 'Form Validation', link: '/form-validation' },
          { label: 'Select2', link: '/form-select2' },
          { label: 'Form Wizard', link: '/form-wizard' }
        ]
      },
      {
        label: 'Tables',
        submenu: true,
        showSubRoute: false,
        icon: <Icon.Columns/>,
        submenuItems: [
          { label: 'Basic Tables', link: '/tables-basic' },
          { label: 'Data Table', link: '/data-tables' }
        ]
      }
    ]
  }, 
  */
  {
    label: 'Ayuda',
    submenuOpen: true,
    showSubRoute: false,
    submenuHdr: 'Help',
    submenuItems: [
      {
        label: 'Manual de Instalación',
        link: '/manuales/manual-instalacion.pdf',
        icon: <Icon.FileText />,
        showSubRoute: false,
        isExternal: true
      },
      {
        label: 'Manual de Impresora',
        link: '/manuales/manual-config-impresora.pdf',
        icon: <Icon.FileText />,
        showSubRoute: false,
        isExternal: true
      },
      {
        label: 'Manual de Usuario',
        link: '/manuales/manual-usuario.pdf',
        icon: <Icon.FileText />,
        showSubRoute: false,
        isExternal: true
      }
    ]
  }
]
