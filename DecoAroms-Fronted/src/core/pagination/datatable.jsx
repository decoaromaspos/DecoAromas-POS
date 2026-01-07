/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */
import React, { useState } from "react";
import { Table } from "antd";

const Datatable = ({ columns, dataSource, pagination }) => {
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  
  const onSelectChange = (newSelectedRowKeys) => {
    console.log("selectedRowKeys changed: ", newSelectedRowKeys);
    setSelectedRowKeys(newSelectedRowKeys);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
  };

  // Configuración de paginación para Ant Design Table
  const tablePagination = pagination ? {
    current: pagination.current,
    pageSize: pagination.pageSize,
    total: pagination.total,
    onChange: pagination.onChange,
    showSizeChanger: false,
    showQuickJumper: false,
    showTotal: (total, range) => 
      `${range[0]}-${range[1]} de ${total} elementos`,
  } : false;

  return (
    <Table
      className="table datanew dataTable no-footer"
      rowSelection={rowSelection}
      columns={columns}
      dataSource={dataSource}
      pagination={tablePagination}
      rowKey={(record) => record.id || record.aromaId || record.productoId}
      locale={{
        emptyText: "No hay datos disponibles"
      }}
    />
  );
};

export default Datatable;