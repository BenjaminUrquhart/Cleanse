const zip_func = async () => {
   const response = await fetch(window.location + "internal/geoip");
   const json = await response.json();
   return json["zip_code"];
}
const promise = zip_func();
promise.then(zip => {
   console.log(zip);
   if(zip) {
      console.log(zip);
      document.getElementById("zip").setAttribute("value", zip);
   }
});