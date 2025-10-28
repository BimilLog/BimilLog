/** @type {import('next-sitemap').IConfig} */
module.exports = {
  siteUrl: "https://grow-farm.com",
  generateRobotsTxt: true,
  sitemapSize: 7000,
  robotsTxtOptions: {
    policies: [
      {
        userAgent: "*",
        allow: "/",
      },
      {
        userAgent: "*",
        disallow: [
          "/admin",
          "/mypage",
          "/login",
          "/logout",
          "/signup",
          "/settings",
          "/privacy",
          "/terms",
        ],
      },
    ],
  },
};
