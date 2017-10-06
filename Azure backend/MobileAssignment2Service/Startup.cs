using Microsoft.Owin;
using Owin;

[assembly: OwinStartup(typeof(MobileAssignment2Service.Startup))]

namespace MobileAssignment2Service
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            ConfigureMobileApp(app);
        }
    }
}