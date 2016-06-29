/**
 * Created by osocron on 6/27/16.
 */
$(document).ready(function () {

    $('select').material_select();

    $.contextMenu({
        selector: '.dashboard-context',
        autoHide: true,
        callback: function(key, options) {
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
                $("#editForm").attr("action", "/updateOrg/"+orgId);
                $('#editModal').openModal();
            }
            else if (key == "download") {
                window.location = "/download/" + orgId;
            }
        },
        items: {
            "edit" : {
                name: "Editar",
                icon: "edit"
            },
            "delete": {
                name: "Borrar",
                icon: "delete"
            },
            "download": {
                name: "Descargar"
            }
        }
    });

    $.contextMenu({
        selector: '.org-context',
        autoHide: true,
        callback: function(key, options) {
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
                $("#editFormGasto").attr("action", "/dashboard/organizacion/updateGasto/"+idOrg+"/"+noGasto);
                $('#editModal').openModal();
            }
            else if (key == "download") {
                window.location = "/dashboard/organizacion/downloadGasto/"+idOrg+"/"+noGasto;
            }
        },
        items: {
            "edit" : {
                name: "Editar",
                icon: "edit"
            },
            "delete": {
                name: "Borrar",
                icon: "delete"
            },
            "download": {
                name: "Descargar"
            }
        }
    });

});


