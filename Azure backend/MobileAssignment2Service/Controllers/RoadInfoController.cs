using System.Linq;
using System.Threading.Tasks;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.Http.OData;
using Microsoft.Azure.Mobile.Server;
using MobileAssignment2Service.DataObjects;
using MobileAssignment2Service.Models;

namespace MobileAssignment2Service.Controllers
{
    // [Authorize]
    public class RoadInfoController : TableController<RoadInfo>
    {
        protected override void Initialize(HttpControllerContext controllerContext)
        {
            base.Initialize(controllerContext);
            MobileAssignment2Context context = new MobileAssignment2Context();
            DomainManager = new EntityDomainManager<RoadInfo>(context, Request);
        }

        // GET tables/RoadInfo
        public IQueryable<RoadInfo> GetAllRoadInfo()
        {
            return Query();
        }

        // GET tables/RoadInfo/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public SingleResult<RoadInfo> GetRoadInfo(string id)
        {
            return Lookup(id);
        }

        // PATCH tables/RoadInfo/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task<RoadInfo> PatchRoadInfo(string id, Delta<RoadInfo> patch)
        {
            return UpdateAsync(id, patch);
        }

        // POST tables/RoadInfo
        public async Task<IHttpActionResult> PostRoadInfo(RoadInfo item)
        {
            RoadInfo current = await InsertAsync(item);
            return CreatedAtRoute("Tables", new { id = current.Id }, current);
        }

        // DELETE tables/RoadInfo/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task DeleteRoadInfo(string id)
        {
            return DeleteAsync(id);
        }
    }
}