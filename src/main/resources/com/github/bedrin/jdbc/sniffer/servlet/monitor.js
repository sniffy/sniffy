console.log('Invoked');
;(function(){

console.log('IIFE');

    /*! contentloaded.min.js - https://github.com/dperini/ContentLoaded - Author: Diego Perini - License: MIT */
    function contentLoaded(b,i){var j=false,k=true,a=b.document,l=a.documentElement,f=a.addEventListener,h=f?'addEventListener':'attachEvent',n=f?'removeEventListener':'detachEvent',g=f?'':'on',c=function(d){if(d.type=='readystatechange'&&a.readyState!='complete')return;(d.type=='load'?b:a)[n](g+d.type,c,false);if(!j&&(j=true))i.call(b,d.type||d)},m=function(){try{l.doScroll('left')}catch(e){setTimeout(m,50);return}c('poll')};if(a.readyState=='complete')i.call(b,'lazy');else{if(!f&&l.doScroll){try{k=!b.frameElement}catch(e){}if(k)m()}a[h](g+'DOMContentLoaded',c,false);a[h](g+'readystatechange',c,false);b[h](g+'load',c,false)}}

    contentLoaded(window, function(){

console.log('Content Loaded');

        var snifferElement = document.getElementById('jdbc-sniffer');
        var sqlQueries = snifferElement.getAttribute('data-sql-queries');
        var requestId = snifferElement.getAttribute('data-request-id');

        var queryCounterDiv = document.createElement('div');
        queryCounterDiv.style.cssText = 'color:red;text-align:center;font-weight:bold;position:fixed;right:20px;bottom:20px;width:24px;height:24px;background-size:100%;z-index:9999999990;opacity:1.0;' +
        'background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAADAklEQVRIibWWzWscdRzGP59hCEGWEEIstZYQQntICbnqxYuI4E0EPcQ/QQ9eJd7s4lEEUfCil+5JL8WLJ98u6qmEkMUXqgQsNYRYllJKCPN4mNmd2aTZouBv2d1nZn6/5/v6fHcFGOxdB10GNoCNwFXCKuSiupRkAZxTCpICrCLHkJFwGDgQ94FfgT1gl3Bn69o2Dvb6a8i7wAvAcqCQdgXw3+FKOAK+S/J2ibwBbI0JTx/yFB5fzMAFYRl5Rb1dAkXX3RBE7IQxHVFqsm6I52BhvpyKT3CK7uxy4vLZ3DwKFy3fKc/OWWk2pXnNxAkFUJFuMrvRnv1sTraXM7BIAXyPHpzNXxvnNBYUdTbGO8DXAtwYXr8sPg88B2wCVwJLj0rvjHUI/ELYQb4Fvtla377rYNhfS3L4+rV3Rt3dg2G/l3DRWhs9pSR1zRIq5RgYEY6Qg6317fvd8zeG/UVh2cGw/wHwMuGHmB/F3YQ/lFGSh8ix8QSppitkAZTAXMK8spiwhmwIz0CeBT8vSUBXkBXxNQBrspFwL/AAeUA4QaqQohZfSuITSk+zkLCgtppCEooSrcZOpS1oEVgUF22aoyOVutQJ2FQntMKcbK7FWkITuh3LnVym86yma1xoGGdi6jExKU7bKZnyeMr7tN+PxTAqgfeAv0heRTeSLE+8bFg7UbdpoXtzgquQA3UX+CLJZyVwAnwEfpKwhF4CrggrwFPAEtID5gllasGfBB4KI+Ao+CdhX/lNvdu0bqWWJeRN8GnkK+En4NbW+vYt/sMa7PVBFjEvgi8BvzsY9t8H3mpGdYXuQ24b9pFDwt/gCDkmnDRcZcic2kvyJHihbvOsES7VI0iAD8uJ+TqHhbAaXG1SQWwfT+7RGeu2Az6d35E05eoI43/oImsDx2OTCfWYjQ1uzHVw3UVjS4/DVAXwKeRL5F7trnVamvdEaGM8FqKeg6kCRyQ3gY8FGAz7BeEysAnZRK9St+kFYAno0aSzHl0h9QC8b012AOyjPwM7wM74b8s/Owp6BPc7ajsAAAAASUVORK5CYII=);';
        queryCounterDiv.innerHTML = sqlQueries;

        document.body.appendChild(queryCounterDiv);

    });

}());