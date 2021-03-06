/**
 * Created by osocron on 6/27/16.
 */
$(document).ready(function () {

    $('select').material_select();

    $.contextMenu({
        selector: '.dashboard-context',
        autoHide: true,
        callback: function (key, options) {
            var orgId = $(this).attr('id');
            if (key === "delete") {
                var conf = confirm("¿Está seguro de borrar la organización?\n\n " +
                    "¡Al hacerlo se perderán todos los gastos guardados!\n");
                if (conf == true) {
                    var xhttp = new XMLHttpRequest();
                    xhttp.onreadystatechange = function () {
                        if (xhttp.readyState == 4 && xhttp.status == 200) {
                            document.getElementById(orgId).remove();
                        }
                    };
                    xhttp.open("POST", "/delOrg/" + orgId, true);
                    xhttp.send();
                }
            }
            else if (key == "edit") {
                $("#editForm").attr("action", "/updateOrg/" + orgId);
                $('#editModal').openModal();
            }
            else if (key == "download") {
                window.location = "/download/" + orgId;
            }
        },
        items: {
            "edit": {
                name: "Editar",
                icon: "paste"
            },
            "delete": {
                name: "Borrar",
                icon: "delete"
            },
            "download": {
                name: "Descargar",
                icon: "edit"
            }
        }
    });

    $.contextMenu({
        selector: '.org-context',
        autoHide: true,
        callback: function (key, options) {
            var noGasto = $(this).attr('id');
            if (key === "delete") {
                var conf = confirm("¿Está seguro de borrar el gasto?\n\n " +
                    "¡Al hacerlo se perderán todos los datos guardados!\n");
                if (conf == true) {
                    var xhttp = new XMLHttpRequest();
                    xhttp.onreadystatechange = function () {
                        if (xhttp.readyState == 4 && xhttp.status == 200) {
                            document.getElementById(noGasto).remove();
                        }
                    };
                    xhttp.open("POST", "/dashboard/organizacion/delGasto/" + noGasto, true);
                    xhttp.send();
                }
            }
            else if (key == "edit") {
                $("#editFormGasto").attr("action", "/dashboard/organizacion/updateGasto/" + idOrg + "/" + noGasto);
                $("#editNoGasto").val(noGasto);
                var elemArr = [];
                $(this).children('td').each(function () {
                    elemArr.push($(this).text());
                });
                $("#editConcepto").val(elemArr[1]);
                $("#editImporte").val(elemArr[2]);
                $("#editMes").val(elemArr[3]);
                $("#editMes").material_select();
                $("#editDia").val(elemArr[4]);
                $("#editDia").material_select();
                $("#editAnio").val(elemArr[5]);
                $("#editAnio").material_select();
                $("#editComprobado").val(elemArr[6]);
                $("#editComprobado").material_select();
                Materialize.updateTextFields();
                $('#editModalOrg').openModal();
            }
            else if (key == "download") {
                window.location = "/dashboard/organizacion/downloadGasto/" + idOrg + "/" + noGasto;
            }
        },
        items: {
            "edit": {
                name: "Editar",
                icon: "paste"
            },
            "delete": {
                name: "Borrar",
                icon: "delete"
            },
            "download": {
                name: "Descargar",
                icon: "edit"
            }
        }
    });


    $.contextMenu({
        selector: '.file-context',
        autoHide: true,
        callback: function (key, options) {
            var idFile = $(this).attr('id');
            if (key === "delete") {
                var conf = confirm("¿Está seguro de borrar los archivos?\n\n " +
                    "¡Al hacerlo se perderán los archivos y no podrán ser recuperados!\n");
                if (conf == true) {
                    var xhttp = new XMLHttpRequest();
                    xhttp.onreadystatechange = function () {
                        if (xhttp.readyState == 4 && xhttp.status == 200) {
                            document.getElementById(idFile).remove();
                        }
                    };
                    xhttp.open("POST", "/dashborad/organizacion/deleteFile/" + fIdOrg + "/" + fNoGasto + "/" + idFile, true);
                    xhttp.send();
                }
            }
            else if (key == "edit") {
                $("#fileEditForm").attr("action", "/dashborad/organizacion/updateFile/" + fIdOrg + "/" + fNoGasto + "/" + idFile);
                $('#fileEditModal').openModal();
            }
            else if (key == "download") {
                window.location = "/dashboard/organizacion/downloadFile/" + fIdOrg + "/" + fNoGasto + "/" + idFile;
            }
        },
        items: {
            "edit": {
                name: "Editar",
                icon: "paste"
            },
            "delete": {
                name: "Borrar",
                icon: "delete"
            },
            "download": {
                name: "Descargar",
                icon: "edit"
            }
        }
    });

});


