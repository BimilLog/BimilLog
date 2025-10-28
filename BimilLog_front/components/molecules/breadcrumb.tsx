"use client";

import { Breadcrumb as FlowbiteBreadcrumb, BreadcrumbItem as FlowbiteBreadcrumbItem } from "flowbite-react";
import { HiHome } from "react-icons/hi";

interface BreadcrumbItem {
  title: string;
  href?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
}


export function Breadcrumb({ items }: BreadcrumbProps) {
  // 구조화된 데이터 생성 (SEO)
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.title,
      item: item.href ? `https://grow-farm.com${item.href}` : undefined,
    })),
  };

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(jsonLd),
        }}
      />
      <FlowbiteBreadcrumb
        aria-label="Breadcrumb navigation"
        className="mb-4 bg-gradient-to-r from-pink-50 via-purple-50 to-indigo-50 dark:from-gray-800 dark:via-gray-800 dark:to-gray-800 px-4 py-3 rounded-xl shadow-sm backdrop-blur-sm border border-purple-100 dark:border-gray-700"
      >
        {items.map((item, index) => (
          <FlowbiteBreadcrumbItem
            key={index}
            href={item.href}
            icon={index === 0 ? HiHome : undefined}
          >
            {item.title}
          </FlowbiteBreadcrumbItem>
        ))}
      </FlowbiteBreadcrumb>
    </>
  );
}
