function saludar(){
  if (window.MintApp) MintApp.toast('Bienvenido a tu app');
  else alert('Bienvenido a tu app');
}
function vibrar(){ if (window.MintApp) MintApp.vibrate(300); }
function toast(){ if (window.MintApp) MintApp.toast('Mensaje desde la app'); }
function dialogo(){ if (window.MintApp) MintApp.showDialog('AppMint', 'Esto es un dialogo nativo'); }