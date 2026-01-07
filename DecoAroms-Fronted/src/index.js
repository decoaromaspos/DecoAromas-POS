
import React from "react";
import { BrowserRouter } from 'react-router-dom';
import ReactDOM from 'react-dom';
import '../node_modules/bootstrap/dist/css/bootstrap.min.css';
import '../node_modules/bootstrap/dist/js/bootstrap.bundle.js';
import { base_path } from "./environment.jsx";
import '../src/style/css/feather.css'
import '../src/style/css/line-awesome.min.css'
import "../src/style/scss/main.scss";
import '../src/style/icons/fontawesome/css/fontawesome.min.css'
import '../src/style/icons/fontawesome/css/all.min.css'
import RouteLoaderWrapper from "./feature-module/loader/RouteLoaderWrapper.jsx";


import { Provider } from "react-redux";
import store from "./core/redux/store.jsx";
import AllRoutes from "./Router/router.jsx";
import { AuthProvider } from "./context/AuthContext";
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const rootElement = document.getElementById('root');



if (rootElement) {
  const root = ReactDOM.createRoot(rootElement);
  root.render(
    <React.StrictMode>
    <Provider store={store} >
      <BrowserRouter basename={base_path}>
      <ToastContainer/>

        <AuthProvider>
          <ToastContainer />
          <RouteLoaderWrapper>
            <AllRoutes />
          </RouteLoaderWrapper>
        </AuthProvider>

      </BrowserRouter>
    </Provider>
  </React.StrictMode>
  );
} else {
  console.error("Element with id 'root' not found.");
}
