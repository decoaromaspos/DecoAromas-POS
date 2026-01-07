import React, { useState, useMemo } from "react";
import Scrollbars from "react-custom-scrollbars-2";
import { Link, useLocation } from "react-router-dom";
import { SidebarData } from "../../core/json/siderbar_data";
import { useAuth } from "../../context/AuthContext";


const Sidebar = () => {
  const { usuario, logout } = useAuth();
  const Location = useLocation();

  const [subOpen, setSubopen] = useState("");
  const [subsidebar, setSubsidebar] = useState("");


  const filteredSidebarData = useMemo(() => {
    const userRole = usuario?.rol;
    if (!userRole) return []; // Si no hay rol, no mostrar nada

    return SidebarData.map(mainLabel => {
      // Filtra los sub-items de esa sección
      const filteredSubmenuItems = mainLabel.submenuItems.filter(item => {
        // Un item es visible si:
        // No tiene una propiedad 'roles' (es para todos)
        // O si el rol del usuario está incluido en su array 'roles'
        return !item.roles || item.roles.includes(userRole);
      });

      return { ...mainLabel, submenuItems: filteredSubmenuItems };
    })
      // Filtrar las secciones principales que quedaron vacías
      .filter(mainLabel => mainLabel.submenuItems.length > 0);

  }, [usuario]);

  const toggleSidebar = (title) => {
    if (title == subOpen) {
      setSubopen("");
    } else {
      setSubopen(title);
    }
  };

  const toggleSubsidebar = (subitem) => {
    if (subitem == subsidebar) {
      setSubsidebar("");
    } else {
      setSubsidebar(subitem);
    }
  };

  return (
    <div>
      <div className="sidebar" id="sidebar">
        <Scrollbars>
          <div className="sidebar-inner slimscroll">
            <div id="sidebar-menu" className="sidebar-menu">
              <ul>



                {filteredSidebarData?.map((mainLabel, index) => (

                  <li className="submenu-open" key={index}>
                    <h6 className="submenu-hdr">{mainLabel?.label}</h6>

                    <ul>
                      {mainLabel?.submenuItems?.map((title, i) => {
                        let link_array = [];
                        title?.submenuItems?.map((link) => {
                          link_array?.push(link?.link);
                          if (link?.submenu) {
                            link?.submenuItems?.map((item) => {
                              link_array?.push(item?.link);
                            });
                          }
                          return link_array;
                        });
                        title.links = link_array;
                        return (
                          <>
                            <li className="submenu" key={i}>

                              {
                                title.action === "logout" ? (
                                  // Si es "Cerrar sesión", crea un enlace <a> que llama a logout()
                                  <a href="#" className="submenu-item" onClick={(e) => {
                                    e.preventDefault();
                                    logout();
                                  }}
                                  >
                                    {title?.icon}
                                    <span>{title?.label}</span>
                                  </a>
                                ) : title.isExternal ? ( 
                                  <a 
                                    href={title.link}
                                    target="_blank" // Para que abra en una pestaña nueva
                                    rel="noopener noreferrer" // Por seguridad
                                    className="submenu-item" // Usamos la misma clase para mantener el estilo
                                  >
                                    {title?.icon}
                                    <span>{title?.label}</span>
                                  </a>

                                ) : (
                                  // Si no, crea el <Link> de navegación normal (es tu código original)
                                  <Link
                                    to={title?.link}
                                    onClick={() => toggleSidebar(title?.label)}
                                    className={`${subOpen == title?.label ? "subdrop" : ""
                                      } ${title?.links?.includes(Location.pathname)
                                        ? "active"
                                        : ""
                                      }`}
                                  >
                                    {title?.icon}
                                    <span>{title?.label}</span>
                                    <span
                                      className={title?.submenu ? "menu-arrow" : ""}
                                    />
                                  </Link>
                                )
                              }



                              <ul
                                style={{
                                  display:
                                    subOpen == title?.label ? "block" : "none",
                                }}
                              >
                                {title?.submenuItems?.map(
                                  (item, titleIndex) => (
                                    <li
                                      className="submenu submenu-two"
                                      key={titleIndex}
                                    >
                                      {/* {item.lebel} */}
                                      <Link
                                        to={item?.link}
                                        className={
                                          item?.submenuItems
                                            ?.map((link) => link?.link)
                                            .includes(Location.pathname) ||
                                            item?.link == Location.pathname
                                            ? "active"
                                            : ""
                                        }
                                        onClick={() => {
                                          toggleSubsidebar(item?.label);
                                        }}
                                      >
                                        {item?.label}
                                        <span
                                          className={
                                            item?.submenu ? "menu-arrow" : ""
                                          }
                                        />
                                      </Link>
                                      <ul
                                        style={{
                                          display:
                                            subsidebar == item?.label
                                              ? "block"
                                              : "none",
                                        }}
                                      >
                                        {item?.submenuItems?.map(
                                          (items, titleIndex) => (
                                            <li key={titleIndex}>
                                              {/* {item.lebel} */}
                                              <Link
                                                to={items?.link}
                                                className={`${subsidebar == items?.label
                                                  ? "submenu-two subdrop"
                                                  : "submenu-two"
                                                  } ${items?.submenuItems
                                                    ?.map((link) => link.link)
                                                    .includes(
                                                      Location.pathname
                                                    ) ||
                                                    items?.link ==
                                                    Location.pathname
                                                    ? "active"
                                                    : ""
                                                  }`}
                                              >
                                                {items?.label}
                                              </Link>
                                            </li>
                                          )
                                        )}
                                      </ul>
                                    </li>
                                  )
                                )}
                              </ul>
                            </li>
                          </>
                        );
                      })}
                    </ul>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Scrollbars>
      </div>
    </div>
  );
};

export default Sidebar;
