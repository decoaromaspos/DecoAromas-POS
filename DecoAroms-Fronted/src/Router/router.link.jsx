import React from "react";
import { Route, Navigate } from "react-router-dom";
import ProductList from "../feature-module/inventory/productlist";
import Dashboard from "../feature-module/dashboard/Dashboard";
import AddProduct from "../feature-module/inventory/addproduct";
import SalesDashbaord from "../feature-module/dashboard/salesdashbaord";
import PrintBarcode from "../feature-module/inventory/printbarcode";
import Grid from "../feature-module/uiinterface/grid";
import Images from "../feature-module/uiinterface/images";
import Lightboxes from "../feature-module/uiinterface/lightbox";
import Media from "../feature-module/uiinterface/media";
import Modals from "../feature-module/uiinterface/modals";
import Offcanvas from "../feature-module/uiinterface/offcanvas";
import Pagination from "../feature-module/uiinterface/pagination";

import Alert from "../feature-module/uiinterface/alert";
import Accordion from "../feature-module/uiinterface/accordion";
import Avatar from "../feature-module/uiinterface/avatar";
import Badges from "../feature-module/uiinterface/badges";
import Borders from "../feature-module/uiinterface/borders";
import Buttons from "../feature-module/uiinterface/buttons";
import ButtonsGroup from "../feature-module/uiinterface/buttonsgroup";
import Popovers from "../feature-module/uiinterface/popover";

import Breadcrumb from "../feature-module/uiinterface/breadcrumb";
import Cards from "../feature-module/uiinterface/cards";
import Dropdowns from "../feature-module/uiinterface/dropdowns";
import Colors from "../feature-module/uiinterface/colors";
import Carousel from "../feature-module/uiinterface/carousel";
import Spinner from "../feature-module/uiinterface/spinner";
import NavTabs from "../feature-module/uiinterface/navtabs";
import Toasts from "../feature-module/uiinterface/toasts";
import Typography from "../feature-module/uiinterface/typography";
import Video from "../feature-module/uiinterface/video";
import Tooltips from "../feature-module/uiinterface/tooltips";
import DragDrop from "../feature-module/uiinterface/advancedui/dragdrop";
import SweetAlert from "../feature-module/uiinterface/sweetalert";
import Progress from "../feature-module/uiinterface/progress";
import Placeholder from "../feature-module/uiinterface/placeholder";
import Rating from "../feature-module/uiinterface/advancedui/rating";
import TextEditor from "../feature-module/uiinterface/advancedui/texteditor";
import Counter from "../feature-module/uiinterface/advancedui/counter";
import Uiscrollbar from "../feature-module/uiinterface/advancedui/uiscrollbar";
import Stickynote from "../feature-module/uiinterface/advancedui/stickynote";
import Timeline from "../feature-module/uiinterface/advancedui/timeline";
import Apexchart from "../feature-module/uiinterface/charts/apexcharts";
import ChartJs from "../feature-module/uiinterface/charts/chartjs";
import RangeSlides from "../feature-module/uiinterface/rangeslider";
import FontawesomeIcons from "../feature-module/uiinterface/icons/fontawesome";
import FeatherIcons from "../feature-module/uiinterface/icons/feathericon";
import IonicIcons from "../feature-module/uiinterface/icons/ionicicons";
import MaterialIcons from "../feature-module/uiinterface/icons/materialicon";
import PE7Icons from "../feature-module/uiinterface/icons/pe7icons";
import SimplelineIcons from "../feature-module/uiinterface/icons/simplelineicon";
import ThemifyIcons from "../feature-module/uiinterface/icons/themify";
import WeatherIcons from "../feature-module/uiinterface/icons/weathericons";
import TypiconIcons from "../feature-module/uiinterface/icons/typicons";
import FlagIcons from "../feature-module/uiinterface/icons/flagicons";

const routes = all_routes;

import ClipBoard from "../feature-module/uiinterface/advancedui/clipboard";
import TablesBasic from "../feature-module/uiinterface/table/tables-basic";
import DataTables from "../feature-module/uiinterface/table/data-tables";
import FormBasicInputs from "../feature-module/uiinterface/forms/formelements/basic-inputs";
import CheckboxRadios from "../feature-module/uiinterface/forms/formelements/checkbox-radios";
import InputGroup from "../feature-module/uiinterface/forms/formelements/input-group";
import GridGutters from "../feature-module/uiinterface/forms/formelements/grid-gutters";
import FormSelect from "../feature-module/uiinterface/forms/formelements/form-select";
import FileUpload from "../feature-module/uiinterface/forms/formelements/fileupload";
import FormMask from "../feature-module/uiinterface/forms/formelements/form-mask";
import FormHorizontal from "../feature-module/uiinterface/forms/formelements/layouts/form-horizontal";
import FormVertical from "../feature-module/uiinterface/forms/formelements/layouts/form-vertical";
import FloatingLabel from "../feature-module/uiinterface/forms/formelements/layouts/floating-label";
import FormValidation from "../feature-module/uiinterface/forms/formelements/layouts/form-validation";
import FormSelect2 from "../feature-module/uiinterface/forms/formelements/layouts/form-select2";
import Ribbon from "../feature-module/uiinterface/advancedui/ribbon";
import FormWizard from "../feature-module/uiinterface/forms/formelements/form-wizard";
import LowStock from "../feature-module/inventory/lowstock";
import CategoryList from "../feature-module/inventory/categorylist";
import SubCategories from "../feature-module/inventory/subcategories";
import EditProduct from "../feature-module/inventory/editproduct";

import Customers from "../feature-module/people/customers";
import SalesReport from "../feature-module/Reports/salesreport";
import InventoryReport from "../feature-module/Reports/inventoryreport";
import SupplierReport from "../feature-module/Reports/supplierreport";
import CustomerReport from "../feature-module/Reports/customerreport";
import ExpenseReport from "../feature-module/Reports/expensereport";
import IncomeReport from "../feature-module/Reports/incomereport";
import TaxReport from "../feature-module/Reports/taxreport";
import ProfitLoss from "../feature-module/Reports/profitloss";
import Pos from "../feature-module/sales/pos";
import SalesList from "../feature-module/sales/saleslist";
import Profile from "../feature-module/pages/profile";
import SigninThree from "../feature-module/pages/login/signinThree";
import RegisterThree from "../feature-module/pages/register/registerThree";
import Forgotpassword from "../feature-module/pages/forgotpassword/forgotpassword";
import ForgotpasswordTwo from "../feature-module/pages/forgotpassword/forgotpasswordTwo";
import ForgotpasswordThree from "../feature-module/pages/forgotpassword/forgotpasswordThree";
import Resetpassword from "../feature-module/pages/resetpassword/resetpassword";
import ResetpasswordTwo from "../feature-module/pages/resetpassword/resetpasswordTwo";
import ResetpasswordThree from "../feature-module/pages/resetpassword/resetpasswordThree";
import EmailVerification from "../feature-module/pages/emailverification/emailverification";
import EmailverificationTwo from "../feature-module/pages/emailverification/emailverificationTwo";
import EmailverificationThree from "../feature-module/pages/emailverification/emailverificationThree";
import Twostepverification from "../feature-module/pages/twostepverification/twostepverification";
import TwostepverificationTwo from "../feature-module/pages/twostepverification/twostepverificationTwo";
import TwostepverificationThree from "../feature-module/pages/twostepverification/twostepverificationThree";
import Lockscreen from "../feature-module/pages/lockscreen";
import Error404 from "../feature-module/pages/errorpages/error404";
import Error500 from "../feature-module/pages/errorpages/error500";
import Blankpage from "../feature-module/pages/blankpage";
import Comingsoon from "../feature-module/pages/comingsoon";
import Undermaintainence from "../feature-module/pages/undermaintainence";




import FamilyList from "../feature-module/inventory/familylist";
import InventoryMovs from "../feature-module/inventory/inventoryMovs";
import Cashboxes from "../feature-module/sales/cashboxes";
import UsuarioList from "../feature-module/people/UsuarioList";
import Configuracion from "../feature-module/settings/Configuracion";
import Reportes from "../feature-module/Reports/Reportes";
import CotizacionesList from "../feature-module/sales/CotizacionesList";
import VentaOnlineList from "../feature-module/sales/VentaOnlineList";


import { all_routes } from "./all_routes";
export const publicRoutes = [
  {
    id: 1,
    path: routes.dashboard,
    name: "home",
    element: <Dashboard />,
    route: Route,
  },
  {
    id: 2,
    path: routes.productlist,
    name: "products",
    element: <ProductList />,
    route: Route,
  },
  {
    id: 3,
    path: routes.addproduct,
    name: "products",
    element: <AddProduct />,
    route: Route,
  },
  {
    id: 4,
    path: routes.salesdashboard,
    name: "salesdashboard",
    element: <SalesDashbaord />,
    route: Route,
  },
  {
    id: 9,
    path: routes.barcode,
    name: "barcode",
    element: <PrintBarcode />,
    route: Route,
  },
  {
    id: 10,
    path: routes.alerts,
    name: "alert",
    element: <Alert />,
    route: Route,
  },
  {
    id: 11,
    path: routes.grid,
    name: "grid",
    element: <Grid />,
    route: Route,
  },

  {
    id: 12,
    path: routes.accordion,
    name: "accordion",
    element: <Accordion />,
    route: Route,
  },
  {
    id: 13,
    path: routes.avatar,
    name: "avatar",
    element: <Avatar />,
    route: Route,
  },
  {
    id: 14,
    path: routes.images,
    name: "images",
    element: <Images />,
    route: Route,
  },

  {
    id: 15,
    path: routes.badges,
    name: "badges",
    element: <Badges />,
    route: Route,
  },
  {
    id: 16,
    path: routes.lightbox,
    name: "lightbox",
    element: <Lightboxes />,
    route: Route,
  },

  {
    id: 17,
    path: routes.borders,
    name: "borders",
    element: <Borders />,
    route: Route,
  },
  {
    id: 18,
    path: routes.media,
    name: "lightbox",
    element: <Media />,
    route: Route,
  },
  {
    id: 19,
    path: routes.buttons,
    name: "borders",
    element: <Buttons />,
    route: Route,
  },
  {
    id: 20,
    path: routes.modals,
    name: "modals",
    element: <Modals />,
    route: Route,
  },
  {
    id: 21,
    path: routes.offcanvas,
    name: "offcanvas",
    element: <Offcanvas />,
    route: Route,
  },
  {
    id: 22,
    path: routes.pagination,
    name: "offcanvas",
    element: <Pagination />,
    route: Route,
  },
  {
    id: 23,
    path: routes.buttonsgroup,
    name: "buttonsgroup",
    element: <ButtonsGroup />,
    route: Route,
  },
  {
    id: 24,
    path: routes.popover,
    name: "buttonsgroup",
    element: <Popovers />,
    route: Route,
  },
  {
    id: 25,
    path: routes.breadcrumb,
    name: "breadcrumb",
    element: <Breadcrumb />,
    route: Route,
  },
  {
    id: 26,
    path: routes.cards,
    name: "cards",
    element: <Cards />,
    route: Route,
  },
  {
    id: 27,
    path: routes.dropdowns,
    name: "dropdowns",
    element: <Dropdowns />,
    route: Route,
  },
  {
    id: 27,
    path: routes.colors,
    name: "colors",
    element: <Colors />,
    route: Route,
  },
  {
    id: 28,
    path: routes.carousel,
    name: "carousel",
    element: <Carousel />,
    route: Route,
  },
  {
    id: 29,
    path: routes.spinner,
    name: "spinner",
    element: <Spinner />,
    route: Route,
  },
  {
    id: 30,
    path: routes.carousel,
    name: "carousel",
    element: <Carousel />,
    route: Route,
  },
  {
    id: 31,
    path: routes.navtabs,
    name: "navtabs",
    element: <NavTabs />,
    route: Route,
  },
  {
    id: 32,
    path: routes.toasts,
    name: "toasts",
    element: <Toasts />,
    route: Route,
  },
  {
    id: 33,
    path: routes.typography,
    name: "typography",
    element: <Typography />,
    route: Route,
  },
  {
    id: 34,
    path: routes.video,
    name: "video",
    element: <Video />,
    route: Route,
  },
  {
    id: 35,
    path: routes.tooltip,
    name: "tooltip",
    element: <Tooltips />,
    route: Route,
  },
  {
    id: 36,
    path: routes.draganddrop,
    name: "draganddrop",
    element: <DragDrop />,
    route: Route,
  },
  {
    id: 37,
    path: routes.sweetalerts,
    name: "sweetalerts",
    element: <SweetAlert />,
    route: Route,
  },
  {
    id: 38,
    path: routes.progress,
    name: "progress",
    element: <Progress />,
    route: Route,
  },
  {
    id: 39,
    path: routes.placeholder,
    name: "placeholder",
    element: <Placeholder />,
    route: Route,
  },
  {
    id: 40,
    path: routes.rating,
    name: "rating",
    element: <Rating />,
  },
  {
    id: 41,
    path: routes.texteditor,
    name: "text-editor",
    element: <TextEditor />,
    route: Route,
  },
  {
    id: 42,
    path: routes.counter,
    name: "counter",
    element: <Counter />,
    route: Route,
  },
  {
    id: 43,
    path: routes.scrollbar,
    name: "scrollbar",
    element: <Uiscrollbar />,
    route: Route,
  },
  {
    id: 43,
    path: routes.clipboard,
    name: "clipboard",
    element: <ClipBoard />,
    route: Route,
  },
  {
    id: 44,
    path: routes.stickynote,
    name: "stickynote",
    element: <Stickynote />,
    route: Route,
  },
  {
    id: 44,
    path: routes.tablebasic,
    name: "tablebasic",
    element: <TablesBasic />,
    route: Route,
  },
  {
    id: 45,
    path: routes.timeline,
    name: "timeline",
    element: <Timeline />,
    route: Route,
  },
  {
    id: 45,
    path: routes.datatable,
    name: "datatable",
    element: <DataTables />,
    route: Route,
  },
  {
    id: 46,
    path: routes.apexchart,
    name: "apex-chart",
    element: <Apexchart />,
    route: Route,
  },

  {
    id: 46,
    path: routes.basicinput,
    name: "formbasicinput",
    element: <FormBasicInputs />,
    route: Route,
  },
  {
    id: 47,
    path: routes.chartjs,
    name: "chart-js",
    element: <ChartJs />,
    route: Route,
  },
  {
    id: 47,
    path: routes.checkboxradio,
    name: "checkboxradio",
    element: <CheckboxRadios />,
    route: Route,
  },
  {
    id: 48,
    path: routes.rangeslider,
    name: "range-slider",
    element: <RangeSlides />,
    route: Route,
  },
  {
    id: 49,
    path: routes.fontawesome,
    name: "fontawesome",
    element: <FontawesomeIcons />,
    route: Route,
  },
  {
    id: 50,
    path: routes.feathericon,
    name: "feathericon",
    element: <FeatherIcons />,
    route: Route,
  },
  {
    id: 51,
    path: routes.ionicicons,
    name: "ionicicons",
    element: <IonicIcons />,
    route: Route,
  },
  {
    id: 52,
    path: routes.materialicons,
    name: "materialicons",
    element: <MaterialIcons />,
    route: Route,
  },
  {
    id: 53,
    path: routes.pe7icons,
    name: "pe7icons",
    element: <PE7Icons />,
    route: Route,
  },
  {
    id: 54,
    path: routes.simpleline,
    name: "simpleline",
    element: <SimplelineIcons />,
    route: Route,
  },
  {
    id: 55,
    path: routes.themifyicons,
    name: "themifyicon",
    element: <ThemifyIcons />,
    route: Route,
  },
  {
    id: 56,
    path: routes.iconweather,
    name: "iconweather",
    element: <WeatherIcons />,
    route: Route,
  },
  {
    id: 57,
    path: routes.typicons,
    name: "typicons",
    element: <TypiconIcons />,
    route: Route,
  },
  {
    id: 58,
    path: routes.flagicons,
    name: "flagicons",
    element: <FlagIcons />,
    route: Route,
  },
  {
    id: 58,
    path: routes.inputgroup,
    name: "inputgroup",
    element: <InputGroup />,
    route: Route,
  },
  {
    id: 59,
    path: routes.ribbon,
    name: "ribbon",
    element: <Ribbon />,
    route: Route,
  },
  {
    id: 49,
    path: routes.gridgutters,
    name: "gridgutters",
    element: <GridGutters />,
    route: Route,
  },
  {
    id: 50,
    path: routes.gridgutters,
    name: "gridgutters",
    element: <GridGutters />,
    route: Route,
  },
  {
    id: 51,
    path: routes.formselect,
    name: "formselect",
    element: <FormSelect />,
    route: Route,
  },
  {
    id: 52,
    path: routes.fileupload,
    name: "fileupload",
    element: <FileUpload />,
    route: Route,
  },
  {
    id: 53,
    path: routes.formmask,
    name: "formmask",
    element: <FormMask />,
    route: Route,
  },
  {
    id: 54,
    path: routes.formhorizontal,
    name: "formhorizontal",
    element: <FormHorizontal />,
    route: Route,
  },
  {
    id: 54,
    path: routes.formvertical,
    name: "formvertical",
    element: <FormVertical />,
    route: Route,
  },
  {
    id: 55,
    path: routes.floatinglabel,
    name: "floatinglabel",
    element: <FloatingLabel />,
    route: Route,
  },
  {
    id: 56,
    path: routes.formvalidation,
    name: "formvalidation",
    element: <FormValidation />,
    route: Route,
  },
  {
    id: 57,
    path: routes.select2,
    name: "select2",
    element: <FormSelect2 />,
    route: Route,
  },
  {
    id: 58,
    path: routes.wizard,
    name: "wizard",
    element: <FormWizard />,
    route: Route,
  },
  {
    id: 59,
    path: routes.lowstock,
    name: "lowstock",
    element: <LowStock />,
    route: Route,
  },
  {
    id: 60,
    path: routes.categorylist,
    name: "categorylist",
    element: <CategoryList />,
    route: Route,
  },
  {
    id: 64,
    path: routes.subcategories,
    name: "subcategories",
    element: <SubCategories />,
    route: Route,
  },
  {
    id: 65,
    path: routes.editproduct + "/:id",
    name: "editproduct",
    element: <EditProduct />,
    route: Route,
  },
  {
    id: 84,
    path: routes.customers,
    name: "customers",
    element: <Customers />,
    route: Route,
  },
  {
    id: 90,
    path: routes.salesreport,
    name: "salesreport",
    element: <SalesReport />,
    route: Route,
  },
  {
    id: 92,
    path: routes.inventoryreport,
    name: "inventoryreport",
    element: <InventoryReport />,
    route: Route,
  },
  {
    id: 94,
    path: routes.supplierreport,
    name: "supplierreport",
    element: <SupplierReport />,
    route: Route,
  },
  {
    id: 95,
    path: routes.customerreport,
    name: "customerreport",
    element: <CustomerReport />,
    route: Route,
  },
  {
    id: 96,
    path: routes.expensereport,
    name: "expensereport",
    element: <ExpenseReport />,
    route: Route,
  },
  {
    id: 97,
    path: routes.incomereport,
    name: "incomereport",
    element: <IncomeReport />,
    route: Route,
  },
  {
    id: 98,
    path: routes.taxreport,
    name: "taxreport",
    element: <TaxReport />,
    route: Route,
  },
  {
    id: 99,
    path: routes.profitloss,
    name: "profitloss",
    element: <ProfitLoss />,
    route: Route,
  },
  {
    id: 99,
    path: routes.pos,
    name: "pos",
    element: <Pos />,
    route: Route,
  },
  {
    id: 102,
    path: routes.saleslist,
    name: "saleslist",
    element: <SalesList />,
    route: Route,
  },
  {
    id: 106,
    path: routes.profile,
    name: "profile",
    element: <Profile />,
    route: Route,
  },
  {
    id: 20,
    path: routes.blankpage,
    name: "blankpage",
    element: <Blankpage />,
    route: Route,
  },
  {
    id: 115,
    path: "/",
    name: "Root",
    element: <Navigate to="/" />,
    route: Route,
  },
  {
    id: 116,
    path: "*",
    name: "NotFound",
    element: <Navigate to="/" />,
    route: Route,
  },





  {
    id: 117,
    path: routes.familylist,
    name: "familylist",
    element: <FamilyList />,
    route: Route,
  },
  {
    id: 118,
    path: routes.inventarymovs,
    name: "inventarymovs",
    element: <InventoryMovs />,
    route: Route,
  },
  {
    id: 119,
    path: routes.cashboxes,
    name: "cashboxes",
    element: <Cashboxes />,
    route: Route,
  },
  {
    id: 120,
    path: routes.usuariolist,
    name: "usuariolist",
    element: <UsuarioList />,
    route: Route,
  },
  {
    id: 121,
    path: routes.configuracion,
    name: "configuracion",
    element: <Configuracion />,
    route: Route,
  },
  {
    id: 122,
    path: routes.reportes,
    name: "reportes",
    element: <Reportes />,
    route: Route,
  },
  {
    id: 123,
    path: routes.cotizaciones,
    name: "cotizacioneslist",
    element: <CotizacionesList />,
    route: Route,
  },
  {
    id: 124,
    path: routes.ventaOnlineList,
    name: "ventaOnlineList",
    element: <VentaOnlineList />,
    route: Route,
  }
];
























export const posRoutes = [
  {
    id: 1,
    path: routes.pos,
    name: "pos",
    element: <Pos />,
    route: Route,
  },
];

export const pagesRoute = [
  {
    id: 3,
    path: routes.signinthree,
    name: "signinthree",
    element: <SigninThree />,
    route: Route,
  },
  {
    id: 6,
    path: routes.registerThree,
    name: "registerThree",
    element: <RegisterThree />,
    route: Route,
  },
  {
    id: 7,
    path: routes.forgotPassword,
    name: "forgotPassword",
    element: <Forgotpassword />,
    route: Route,
  },
  {
    id: 7,
    path: routes.forgotPasswordTwo,
    name: "forgotPasswordTwo",
    element: <ForgotpasswordTwo />,
    route: Route,
  },
  {
    id: 8,
    path: routes.forgotPasswordThree,
    name: "forgotPasswordThree",
    element: <ForgotpasswordThree />,
    route: Route,
  },
  {
    id: 9,
    path: routes.resetpassword,
    name: "resetpassword",
    element: <Resetpassword />,
    route: Route,
  },
  {
    id: 10,
    path: routes.resetpasswordTwo,
    name: "resetpasswordTwo",
    element: <ResetpasswordTwo />,
    route: Route,
  },
  {
    id: 11,
    path: routes.resetpasswordThree,
    name: "resetpasswordThree",
    element: <ResetpasswordThree />,
    route: Route,
  },
  {
    id: 12,
    path: routes.emailverification,
    name: "emailverification",
    element: <EmailVerification />,
    route: Route,
  },
  {
    id: 12,
    path: routes.emailverificationTwo,
    name: "emailverificationTwo",
    element: <EmailverificationTwo />,
    route: Route,
  },
  {
    id: 13,
    path: routes.emailverificationThree,
    name: "emailverificationThree",
    element: <EmailverificationThree />,
    route: Route,
  },
  {
    id: 14,
    path: routes.twostepverification,
    name: "twostepverification",
    element: <Twostepverification />,
    route: Route,
  },
  {
    id: 15,
    path: routes.twostepverificationTwo,
    name: "twostepverificationTwo",
    element: <TwostepverificationTwo />,
    route: Route,
  },
  {
    id: 16,
    path: routes.twostepverificationThree,
    name: "twostepverificationThree",
    element: <TwostepverificationThree />,
    route: Route,
  },
  {
    id: 17,
    path: routes.lockscreen,
    name: "lockscreen",
    element: <Lockscreen />,
    route: Route,
  },
  {
    id: 18,
    path: routes.error404,
    name: "error404",
    element: <Error404 />,
    route: Route,
  },
  {
    id: 19,
    path: routes.error500,
    name: "error500",
    element: <Error500 />,
    route: Route,
  },
  {
    id: 20,
    path: routes.comingsoon,
    name: "comingsoon",
    element: <Comingsoon />,
    route: Route,
  },
  {
    id: 21,
    path: routes.undermaintenance,
    name: "undermaintenance",
    element: <Undermaintainence />,
    route: Route,
  },
];
