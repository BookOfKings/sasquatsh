using GameNight.Api.Middleware;
using GameNight.Api.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Repository services
builder.Services.AddScoped<IUserRepository, UserRepository>();
builder.Services.AddScoped<IEventRepository, EventRepository>();

// Firebase authentication service
builder.Services.AddSingleton<IFirebaseAuthService, FirebaseAuthService>();

// Authorization (for [Authorize] attribute)
builder.Services.AddAuthorization();

builder.Services.AddCors(options =>
{
    options.AddPolicy("frontend", policy =>
    {
        policy.AllowAnyOrigin()
            .AllowAnyHeader()
            .AllowAnyMethod();
    });
});

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors("frontend");

// Firebase authentication middleware (before authorization)
app.UseFirebaseAuth();

app.UseAuthorization();

app.MapControllers();

app.Run();
