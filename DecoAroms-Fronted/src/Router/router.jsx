import React from "react";
import { Route, Routes, Outlet } from "react-router-dom";
import Header from "../InitialPage/Sidebar/Header";
import Sidebar from "../InitialPage/Sidebar/Sidebar";
import { pagesRoute, posRoutes, publicRoutes } from "./router.link";
import { useSelector } from "react-redux";
import ProtectedRoute from "./ProtectedRoute";


const AllRoutes = () => {
  const data = useSelector((state) => state.toggle_header);

  const HeaderLayout = () => (
    <div className={`main-wrapper ${data ? "header-collapse" : ""}`}>
      <Header />
      <Sidebar />
      <Outlet />
    </div>
  );

  const Authpages = () => (
    <div className={data ? "header-collapse" : ""}>
      <Outlet />
    </div>
  );

  const Pospages = () => (
    <div>
      <Header />
      <Outlet />
    </div>
  );

  return (
    <div>
      <Routes>


        <Route 
          path="/pos" 
          element={
            <ProtectedRoute>
              <Pospages />
            </ProtectedRoute>
          }>
          {posRoutes.map((route, id) => (
            <Route path={route.path} element={route.element} key={id} />
          ))}
        </Route>

        <Route 
          path={"/"} 
            element={
              <ProtectedRoute>
                <HeaderLayout />
              </ProtectedRoute>
            }>
          {publicRoutes.map((route, id) => (
            <Route path={route.path} element={route.element} key={id} />
          ))}
        </Route>

        <Route 
          path={"/"} 
          element={
            <Authpages />
          }>
          {pagesRoute.map((route, id) => (
            <Route path={route.path} element={route.element} key={id} />
          ))}
        </Route>

      </Routes>
    </div>
  );
};
export default AllRoutes;
